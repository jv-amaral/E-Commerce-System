package model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/** Persistência em arquivo binário dos usuários (Users.bin) e lojas (Stores.bin). */
public class ArquivoUsuario {
    private static final String ARQUIVO_USUARIOS = "Users.bin";
    private static final String ARQUIVO_LOJAS = "Stores.bin";

    private ArquivoUsuario() {
    }

    public static User lerUsuario(int key, String password)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        ArquivoBinario.validarChave(key);
        ArquivoBinario.validarTexto(password, ArquivoBinario.TAMANHO_STRING, "Senha");
        File arq = new File(ARQUIVO_USUARIOS);
        if (!arq.exists()) {
            throw new DadosNaoEncontradosException("Usuário não cadastrado!");
        }

        try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {
            ArquivoBinario.conferirRegistros(raf, ArquivoBinario.TAM_REG_USUARIO, ARQUIVO_USUARIOS);
            while (raf.getFilePointer() < raf.length()) {
                int keyRead = raf.readInt();
                if (keyRead == key) {
                    String passwordRead = ArquivoBinario.lerString(raf);
                    if (!passwordRead.equals(password)) {
                        throw new SenhaIncorretaException("Senha incorreta!");
                    }

                    char userType = raf.readChar();
                    User user;
                    try {
                        if (userType == '0') {
                            user = new User(key, password);
                        } else if (userType == '1') {
                            user = new UserExtended(key, password);
                            try {
                                Loja loja = buscarLoja(key);
                                ((UserExtended) user).setStoreName(loja.getNome());
                                ((UserExtended) user).setStoreCategory(loja.getCategoria());
                            } catch (DadosNaoEncontradosException e) {
                                // Loja ainda não cadastrada para o lojista.
                            }
                        } else {
                            throw new IOException("Tipo de usuário inválido em Users.bin.");
                        }

                        for (int i = 0; i < ArquivoBinario.TAMANHO_CARRINHO; i++) {
                            user.setCartProductsRRN(raf.readInt(), i);
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IOException("O cadastro contém dados inválidos.", e);
                    }
                    return user;
                }
                raf.skipBytes(ArquivoBinario.TAM_REG_USUARIO - Integer.BYTES);
            }
        }
        throw new DadosNaoEncontradosException("Usuário não cadastrado!");
    }

    public static boolean existeUsuario(int key) throws IOException {
        File arq = new File(ARQUIVO_USUARIOS);
        if (!arq.exists()) {
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {
            ArquivoBinario.conferirRegistros(raf, ArquivoBinario.TAM_REG_USUARIO, ARQUIVO_USUARIOS);
            while (raf.getFilePointer() < raf.length()) {
                if (raf.readInt() == key) {
                    return true;
                }
                raf.skipBytes(ArquivoBinario.TAM_REG_USUARIO - Integer.BYTES);
            }
        }
        return false;
    }

    public static void salvarNovoUsuario(User user) throws IOException {
        if (user == null) {
            throw new IllegalArgumentException("O usuário não pode ser nulo.");
        }
        user.validarDados();

        if (user instanceof UserExtended) {
            salvarNovoLojista((UserExtended) user);
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(ARQUIVO_USUARIOS, "rw")) {
            conferirNovoUsuario(raf, user.getKey());
            long tamanhoAnterior = raf.length();
            try {
                raf.seek(tamanhoAnterior);
                escreverRegistroUsuario(raf, user);
            } catch (IOException | RuntimeException e) {
                ArquivoBinario.restaurarTamanho(raf, tamanhoAnterior, e);
                throw e;
            }
        }
    }

    private static void salvarNovoLojista(UserExtended lojista) throws IOException {
        try (RandomAccessFile users = new RandomAccessFile(ARQUIVO_USUARIOS, "rw");
                RandomAccessFile stores = new RandomAccessFile(ARQUIVO_LOJAS, "rw")) {
            conferirNovoUsuario(users, lojista.getKey());
            ArquivoBinario.conferirRegistros(stores, ArquivoBinario.TAM_REG_LOJA, ARQUIVO_LOJAS);
            while (stores.getFilePointer() < stores.length()) {
                if (stores.readInt() == lojista.getKey()) {
                    throw new IllegalArgumentException("Já existe uma loja com este NROUSP.");
                }
                stores.skipBytes(2 * ArquivoBinario.TAMANHO_STRING);
            }
            long tamanhoUsers = users.length();
            long tamanhoStores = stores.length();
            try {
                users.seek(tamanhoUsers);
                escreverRegistroUsuario(users, lojista);
                stores.seek(tamanhoStores);
                stores.writeInt(lojista.getKey());
                ArquivoBinario.escreverString(stores, lojista.getStoreName());
                ArquivoBinario.escreverString(stores, lojista.getStoreCategory());
            } catch (IOException | RuntimeException e) {
                ArquivoBinario.restaurarTamanho(users, tamanhoUsers, e);
                ArquivoBinario.restaurarTamanho(stores, tamanhoStores, e);
                throw e;
            }
        }
    }

    public static void salvarCarrinho(int key, int[] rrns) throws IOException {
        ArquivoBinario.validarChave(key);
        if (rrns == null || rrns.length != ArquivoBinario.TAMANHO_CARRINHO) {
            throw new IllegalArgumentException("O carrinho deve possuir cinco posições.");
        }
        if (!new File(ARQUIVO_USUARIOS).exists()) {
            throw new IOException("Usuário não cadastrado em Users.bin.");
        }
        try (RandomAccessFile raf = new RandomAccessFile(ARQUIVO_USUARIOS, "rw")) {
            ArquivoBinario.conferirRegistros(raf, ArquivoBinario.TAM_REG_USUARIO, ARQUIVO_USUARIOS);
            while (raf.getFilePointer() < raf.length()) {
                int keyRead = raf.readInt();
                if (keyRead == key) {
                    raf.skipBytes(ArquivoBinario.TAMANHO_STRING + Character.BYTES);
                    for (int rrn : rrns) {
                        raf.writeInt(rrn);
                    }
                    return;
                }
                raf.skipBytes(ArquivoBinario.TAM_REG_USUARIO - Integer.BYTES);
            }
        }
        throw new IOException("Usuário não cadastrado em Users.bin.");
    }

    public static List<Loja> listarLojas() throws IOException {
        List<Loja> lojas = new ArrayList<>();
        if (new File(ARQUIVO_LOJAS).exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(ARQUIVO_LOJAS, "r")) {
                ArquivoBinario.conferirRegistros(raf, ArquivoBinario.TAM_REG_LOJA, ARQUIVO_LOJAS);
                while (raf.getFilePointer() < raf.length()) {
                    lojas.add(new Loja(
                            raf.readInt(),
                            ArquivoBinario.lerString(raf),
                            ArquivoBinario.lerString(raf)));
                }
            }
        }
        return lojas;
    }

    public static Loja buscarLoja(int key) throws IOException, DadosNaoEncontradosException {
        ArquivoBinario.validarChave(key);
        if (!new File(ARQUIVO_LOJAS).exists()) {
            throw new DadosNaoEncontradosException("Loja não cadastrada!");
        }
        try (RandomAccessFile raf = new RandomAccessFile(ARQUIVO_LOJAS, "r")) {
            ArquivoBinario.conferirRegistros(raf, ArquivoBinario.TAM_REG_LOJA, ARQUIVO_LOJAS);
            while (raf.getFilePointer() < raf.length()) {
                int keyRead = raf.readInt();
                if (keyRead == key) {
                    return new Loja(
                            keyRead,
                            ArquivoBinario.lerString(raf),
                            ArquivoBinario.lerString(raf));
                }
                raf.skipBytes(2 * ArquivoBinario.TAMANHO_STRING);
            }
        }
        throw new DadosNaoEncontradosException("Loja não cadastrada!");
    }

    private static void conferirNovoUsuario(RandomAccessFile raf, int key) throws IOException {
        ArquivoBinario.conferirRegistros(raf, ArquivoBinario.TAM_REG_USUARIO, ARQUIVO_USUARIOS);
        while (raf.getFilePointer() < raf.length()) {
            if (raf.readInt() == key) {
                throw new IllegalArgumentException("Já existe um usuário cadastrado com este NROUSP!");
            }
            raf.skipBytes(ArquivoBinario.TAM_REG_USUARIO - Integer.BYTES);
        }
    }

    private static void escreverRegistroUsuario(RandomAccessFile raf, User user) throws IOException {
        raf.writeInt(user.getKey());
        ArquivoBinario.escreverString(raf, user.getPassword());
        raf.writeChar(user.getTipoRegistro());
        for (int rrn : user.getCartProductsRRN()) {
            raf.writeInt(rrn);
        }
    }
}
