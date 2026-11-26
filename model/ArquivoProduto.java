package model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Persistência em arquivo binário dos produtos (Products.bin). */
public class ArquivoProduto {
    private static final String NOME_ARQUIVO = "Products.bin";
    private static final int TAMANHO_INDICE_DE_REMOCAO = 1;
    private static final int TAMANHO_STRING = ArquivoBinario.TAMANHO_STRING;
    private static final int TAMANHO_DESCRICAO = ArquivoBinario.TAMANHO_DESCRICAO;
    private static final int TAMANHO_CABECALHO = ArquivoBinario.TAM_CAB_PRODUTO;

    // flag + idUsuario + idProduto + preco + estoque + nome + descricao
    private static final int TAMANHO_REGISTRO = TAMANHO_INDICE_DE_REMOCAO + 2 * Integer.BYTES
            + TAMANHO_STRING + Double.BYTES + Integer.BYTES + TAMANHO_DESCRICAO;
    private static final byte FLAG_ATIVO = 0;
    private static final byte FLAG_REMOVIDO = 1;

    private static void prepararArquivo(RandomAccessFile arquivo) throws IOException {
        if (arquivo.length() == 0) {
            arquivo.seek(0);
            arquivo.writeInt(1);
            arquivo.writeInt(-1);
        }

        if (arquivo.length() < TAMANHO_CABECALHO
                || (arquivo.length() - TAMANHO_CABECALHO) % TAMANHO_REGISTRO != 0) {
            throw new IOException("O arquivo não corresponde ao formato de registros de 181 bytes.");
        }

        arquivo.seek(0);
        int proximoIdProduto = arquivo.readInt();
        int topoPilha = arquivo.readInt();

        if (proximoIdProduto <= 0 || topoPilha != -1) {
            throw new IOException("Cabeçalho inválido ou pilha de remoções ainda não suportada.");
        }
    }

    private static byte[] prepararTexto(String texto, int tamanho) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O texto não pode estar vazio.");
        }

        byte[] bytesTexto = texto.getBytes(StandardCharsets.UTF_8);

        if (bytesTexto.length > tamanho || texto.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "O texto deve caber em " + tamanho + " bytes e não conter caractere nulo.");
        }

        byte[] campo = new byte[tamanho];
        System.arraycopy(bytesTexto, 0, campo, 0, bytesTexto.length);
        return campo;
    }

    private static String lerTexto(RandomAccessFile arquivo, int tamanho) throws IOException {
        byte[] campo = new byte[tamanho];
        arquivo.readFully(campo);
        int tamanhoReal = 0;

        while (tamanhoReal < tamanho && campo[tamanhoReal] != 0) {
            tamanhoReal++;
        }
        return new String(campo, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    private static byte lerFlag(RandomAccessFile arquivo) throws IOException {
        byte flag = arquivo.readByte();

        if (flag != FLAG_ATIVO && flag != FLAG_REMOVIDO) {
            throw new IOException("Flag de remoção inválida.");
        }
        return flag;
    }

    private static void escreverRegistro(RandomAccessFile arquivo, Produto produto) throws IOException {
        if (produto.getRemovido() != FLAG_ATIVO) {
            throw new IllegalArgumentException("Somente produtos ativos podem ser gravados por este método.");
        }

        byte[] nome = prepararTexto(produto.getNome(), TAMANHO_STRING);
        byte[] descricao = prepararTexto(produto.getDescricao(), TAMANHO_DESCRICAO);
        arquivo.writeByte(produto.getRemovido());
        arquivo.writeInt(produto.getIdUsuario());
        arquivo.writeInt(produto.getIdProduto());
        arquivo.writeDouble(produto.getPreco());
        arquivo.writeInt(produto.getQuantidade());
        arquivo.write(nome);
        arquivo.write(descricao);
    }

    private static Produto lerRegistro(RandomAccessFile arquivo) throws IOException {
        long inicio = arquivo.getFilePointer();
        byte flag = lerFlag(arquivo);

        if (flag == FLAG_REMOVIDO) {
            arquivo.seek(inicio + TAMANHO_REGISTRO);
            return null;
        }
        int idUsuario = arquivo.readInt();
        int idProduto = arquivo.readInt();
        double preco = arquivo.readDouble();
        int quantidade = arquivo.readInt();
        String nome = lerTexto(arquivo, TAMANHO_STRING);
        String descricao = lerTexto(arquivo, TAMANHO_DESCRICAO);

        try {
            return new Produto(flag, nome, descricao, preco, quantidade, idProduto, idUsuario);
        } catch (IllegalArgumentException erro) {
            throw new IOException("O registro contém dados de produto inválidos.", erro);
        }
    }

    private static long localizarRegistro(RandomAccessFile arquivo, int idProduto) throws IOException {
        if (idProduto <= 0) {
            throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
        }

        for (long posicao = TAMANHO_CABECALHO; posicao < arquivo.length(); posicao += TAMANHO_REGISTRO) {
            arquivo.seek(posicao);
            byte flag = lerFlag(arquivo);
            arquivo.skipBytes(Integer.BYTES);
            int idLido = arquivo.readInt();

            if (flag == FLAG_ATIVO && idLido == idProduto) {
                return posicao;
            }
        }
        throw new IllegalArgumentException("Produto não encontrado: " + idProduto);
    }

    public static Produto cadastrar(String nome, String descricao, double preco,
            int quantidadeEstoque, int idUsuario) throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            arquivo.seek(0);
            int idGerado = arquivo.readInt();

            if (idGerado == Integer.MAX_VALUE) {
                throw new IllegalStateException("O contador de IDs atingiu o limite.");
            }
            Produto produto = new Produto(FLAG_ATIVO, nome, descricao, preco, quantidadeEstoque, idGerado, idUsuario);

            arquivo.seek(arquivo.length());
            escreverRegistro(arquivo, produto);
            arquivo.seek(0);
            arquivo.writeInt(idGerado + 1);
            return produto;
        }
    }

    public static Produto buscarPorId(int idProduto) throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            long posicao = localizarRegistro(arquivo, idProduto);
            arquivo.seek(posicao);
            return lerRegistro(arquivo);
        }
    }

    public static int rrnDisponivel(int idProduto) throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "r")) {
            ArquivoBinario.conferirProdutos(arquivo);
            long pos = localizarRegistro(arquivo, idProduto);
            arquivo.seek(pos);
            if (lerRegistro(arquivo).getQuantidade() <= 0) {
                throw new IllegalArgumentException("Produto sem estoque.");
            }
            return (int) ((pos - TAMANHO_CABECALHO) / TAMANHO_REGISTRO);
        }
    }

    public static Produto buscarPorRRN(int rrn) throws IOException {
        if (!new File(NOME_ARQUIVO).exists()) {
            return null;
        }
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "r")) {
            ArquivoBinario.conferirProdutos(arquivo);
            long pos = TAMANHO_CABECALHO + (long) rrn * TAMANHO_REGISTRO;
            if (rrn < 0 || pos >= arquivo.length()) {
                return null;
            }
            arquivo.seek(pos);
            return lerRegistro(arquivo);
        }
    }

    /** Consulta os itens do carrinho distinguindo produtos ativos, removidos pelo vendedor e indisponíveis. */
    public static List<ItemCarrinho> consultarItensPorRRNs(int[] rrns) throws IOException {
        List<ItemCarrinho> itens = new ArrayList<>();
        if (rrns == null) {
            return itens;
        }
        File arq = new File(NOME_ARQUIVO);
        if (!arq.exists()) {
            for (int pos = 0; pos < rrns.length; pos++) {
                if (rrns[pos] != -1) {
                    itens.add(ItemCarrinho.indisponivel(pos));
                }
            }
            return itens;
        }
        try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {
            ArquivoBinario.conferirProdutos(raf);
            for (int pos = 0; pos < rrns.length; pos++) {
                int rrn = rrns[pos];
                if (rrn == -1) {
                    continue;
                }
                long offset = TAMANHO_CABECALHO + (long) rrn * TAMANHO_REGISTRO;
                if (rrn < 0 || offset + TAMANHO_REGISTRO > raf.length()) {
                    itens.add(ItemCarrinho.indisponivel(pos));
                    continue;
                }
                raf.seek(offset);
                byte flag = lerFlag(raf);
                int idUsuario = raf.readInt();
                int idProduto = raf.readInt();
                if (flag == FLAG_REMOVIDO) {
                    itens.add(ItemCarrinho.removidoPeloVendedor(pos, idProduto));
                    continue;
                }
                double preco = raf.readDouble();
                int estoque = raf.readInt();
                String nome = lerTexto(raf, TAMANHO_STRING);
                String descricao = lerTexto(raf, TAMANHO_DESCRICAO);
                try {
                    Produto produto = new Produto(flag, nome, descricao, preco, estoque, idProduto, idUsuario);
                    itens.add(ItemCarrinho.ativo(pos, produto));
                } catch (IllegalArgumentException e) {
                    throw new IOException("O registro contém dados de produto inválidos.", e);
                }
            }
        }
        return itens;
    }

    /** Localiza no vetor de RRNs do carrinho a primeira posição cujo registro corresponde ao ID informado. */
    public static int localizarPosicaoNoCarrinhoPorId(int[] rrns, int idProduto) throws IOException {
        if (idProduto <= 0) {
            throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
        }
        try (RandomAccessFile raf = new RandomAccessFile(NOME_ARQUIVO, "r")) {
            ArquivoBinario.conferirProdutos(raf);
            for (int i = 0; i < rrns.length; i++) {
                int rrn = rrns[i];
                if (rrn == -1) {
                    continue;
                }
                long posRegistro = TAMANHO_CABECALHO + (long) rrn * TAMANHO_REGISTRO;
                if (rrn < 0 || posRegistro + TAMANHO_REGISTRO > raf.length()) {
                    continue;
                }
                raf.seek(posRegistro + 1 + Integer.BYTES);
                int idLido = raf.readInt();
                if (idLido == idProduto) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static List<Produto> listarTodos() throws IOException {
        List<Produto> produtos = new ArrayList<>();

        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            arquivo.seek(TAMANHO_CABECALHO);

            while (arquivo.getFilePointer() < arquivo.length()) {
                Produto produto = lerRegistro(arquivo);
                if (produto != null) {
                    produtos.add(produto);
                }
            }
        }
        return produtos;
    }

    public static List<Produto> listarPorUsuario(int idUsuario) throws IOException {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException("O ID do usuário deve ser maior que zero.");
        }
        List<Produto> produtosDoUsuario = new ArrayList<>();

        for (Produto produto : listarTodos()) {
            if (produto.getIdUsuario() == idUsuario) {
                produtosDoUsuario.add(produto);
            }
        }
        return produtosDoUsuario;
    }

    public static void atualizar(Produto produto) throws IOException {
        if (produto == null) {
            throw new IllegalArgumentException("O produto não pode ser nulo.");
        }

        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            long posicao = localizarRegistro(arquivo, produto.getIdProduto());
            arquivo.seek(posicao);
            Produto original = lerRegistro(arquivo);

            if (original.getIdUsuario() != produto.getIdUsuario()) {
                throw new IllegalArgumentException("Não é possível trocar o vendedor do produto.");
            }
            arquivo.seek(posicao);
            escreverRegistro(arquivo, produto);
        }
    }

    public static void removerRegistro(int idProduto) throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            remover_registro(arquivo, idProduto);
        }
    }

    public static void remover_registro(RandomAccessFile arquivo, int idProduto)
            throws IOException {
        prepararArquivo(arquivo);
        long posicao = localizarRegistro(arquivo, idProduto);
        arquivo.seek(posicao);
        arquivo.writeByte(FLAG_REMOVIDO);
    }
}
