import java.util.Scanner;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class FuncionalidadeInvalidaException extends Exception {
    public FuncionalidadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}

public class User {

    // Strings com no máximo 60 bytes

    protected int key;
    protected String password;
    protected int[] cartProductsRRN;

    // Construtores:
    public User(int key, String password) {
        // utilizado para criar um usuário novo no sistema
        this.key = key;
        this.password = password;
        cartProductsRRN = new int[] { -1, -1, -1, -1, -1 };
    }

    // Setters e Getters:
    public void setKey(int key) {
        this.key = key;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCartProductsRRN(int RRN, int pos) {
        this.cartProductsRRN[pos] = RRN;
    }

    public int getKey() {
        return key;
    }

    public String getPassword() {
        return password;
    }

    // Demais métodos:

    protected void exibirCatalogo() {
        byte removido;
        String nome = new String();
        String descricao = new String();
        double preco;
        int estoque, id;

        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "rw")) {

            raf.seek(8);
            while (true) {
                try {
                    removido = raf.readByte();
                    raf.skipBytes(4);
                    id = raf.readInt();
                    preco = raf.readDouble();
                    estoque = raf.readInt();
                    nome = MarketPlace.lerString(raf);
                    descricao = MarketPlace.lerString(raf, 100);

                    if (removido == 1 || estoque == 0) {
                        continue;
                    }

                    System.out.printf("%nProduto: %s%nPreço: %f%nEstoque: %d%nID: %d%nDescricao: %s%n", nome, preco,
                            estoque, id, descricao);

                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("\nUse addcart <id_Produto> para adicionar um produto ao carrinho\n[1] Voltar\n");

        Scanner input = new Scanner(System.in);
        
        while (true) {
            String comando = input.next();
            if (comando.equals("addcart")) {
                int idProduto = input.nextInt();
                this.adicionarAoCarrinho(idProduto);
            } else if (comando.equals("1")) {
                break;
            }
        }
        this.painelInterativo();

    }

    protected void exibirLojas() {

        String name = new String();
        String category = new String();
        int sellerid; 

        try (RandomAccessFile raf = new RandomAccessFile("Stores.bin", "rw")) {

            while (true) {
                try {

                    sellerid = raf.readInt(); 
                    name = MarketPlace.lerString(raf);
                    category = MarketPlace.lerString(raf);

                    System.out.printf("%nLoja: %s%nCategoria: %s%nID Vendedor: %d%n", name, category, sellerid);

                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("\nUse openstore <id_Vendedor> para adicionar um produto ao carrinho\n[1] Voltar\n");
        Scanner input = new Scanner(System.in);

        while (true) {
            String comando = input.next();
            if (comando.equals("openstore")) {
                int key = input.nextInt(); 
                MarketPlace.exibirLoja(key);
            } else if (comando.equals("1")) {
                break;
            }
        }
        this.painelInterativo();
    }

    protected void exibirCarrinho() {

        int pos, id, estoque;
        String nome = new String();
        String descricao = new String();
        double preco;

        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "rw")) {

            for (int i = 0; i < 5; i++) {

                if (cartProductsRRN[i] == -1)
                    continue;

                pos = 8 + cartProductsRRN[i] * 181 + 5;
                raf.seek(pos);

                id = raf.readInt();
                preco = raf.readDouble();
                estoque = raf.readInt();
                nome = MarketPlace.lerString(raf);
                descricao = MarketPlace.lerString(raf, 100);

                System.out.printf("%nProduto: %s%nPreço: %f%nEstoque: %d%nID: %d%nDescricao: %s%n", nome, preco,
                        estoque, id, descricao);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("\nUse cartrem <id_Produto> para remover um produto do carrinho\n[1] Voltar\n");
        Scanner input = new Scanner(System.in);

        while (true) {
            String comando = input.next();
            if (comando.equals("cartrem")) {
                int idProduct = input.nextInt(); 
                this.removerDoCarrinho(idProduct);
            } else if (comando.equals("1")) {
                break;
            }
        }
        this.painelInterativo();

    }

    public boolean adicionarAoCarrinho(int idProduto) {
        int posLivre = -1;
        for (int i = 0; i < this.cartProductsRRN.length; i++) {
            if (this.cartProductsRRN[i] == -1) {
                posLivre = i;
                break;
            }
        }

        if (posLivre == -1) {
            System.out.print("Seu carrinho está cheio!\n");
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "r")) {
            int rrn = 0;

            while (8 + (long) rrn * 181 + 9 <= raf.length()) {
                raf.seek(8 + (long) rrn * 181);

                byte removido = raf.readByte();
                raf.skipBytes(4);
                int idLido = raf.readInt();

                if (removido == 0 && idLido == idProduto) {
                    this.cartProductsRRN[posLivre] = rrn;
                    System.out.printf("Produto ID %d adicionado ao carrinho!%n", idProduto);
                    return true;
                }
                rrn++;
            }

            System.out.printf("Produto com ID %d não foi encontrado.%n", idProduto);
            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removerDoCarrinho(int idProduto) {
        boolean vazio = true;
        for (int i = 0; i < this.cartProductsRRN.length; i++) {
            if (this.cartProductsRRN[i] != -1) {
                vazio = false;
                break;
            }
        }

        if (vazio) {
            System.out.print("Seu carrinho está vazio!\n");
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "rw")) {
            for (int i = 0; i < this.cartProductsRRN.length; i++) {
                int rrn = this.cartProductsRRN[i];
                if (rrn == -1) {
                    continue;
                }

                long posRegistro = 8 + (long) rrn * 181;
                if (posRegistro + 9 > raf.length()) {
                    continue;
                }

                raf.seek(posRegistro);
                byte removido = raf.readByte();
                raf.skipBytes(4);
                int idLido = raf.readInt();

                if (idLido == idProduto) {
                    // 1. Muda a flag de removido para 1 no arquivo (1 byte no início do registro)
                    raf.seek(posRegistro);
                    raf.writeByte(1);

                    // 2. Coloca o RRN referente ao removido na lista cartProductsRRN como -1
                    this.cartProductsRRN[i] = -1;

                    System.out.printf("Produto ID %d removido do carrinho com sucesso!%n", idProduto);
                    return true;
                }
            }

            System.out.printf("Produto com ID %d não foi encontrado no carrinho.%n", idProduto);
            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void painelInterativo() {

        System.out.print("MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n[3] Meu Carrinho\n[4] Sair\n");
        Scanner input = new Scanner(System.in);

        int func = input.nextInt();

        if (func < 1 || func > 4) {
            System.out.println("Funcionalidade Inválida!");
            this.painelInterativo();
            return;
        }

        switch (func) {

            case 1:
                exibirCatalogo();
                break;

            case 2:
                exibirLojas();
                break;

            case 3:
                this.exibirCarrinho();
                break;

            case 4:
                userDataSaving();
                System.exit(0);
        }

    }

    public void newUserDataSaving() {

        try (RandomAccessFile raf = new RandomAccessFile("Users.bin", "rw")) {

            raf.seek(raf.length());
            raf.writeInt(key);
            MarketPlace.escreverString(raf, password);
            raf.writeChar('0');

            for (int i = 0; i < 5; i++) {
                raf.writeInt(cartProductsRRN[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void userDataSaving() {

        int keyRead;

        try (RandomAccessFile raf = new RandomAccessFile("Users.bin", "rw")) {

            while (true) {
                try {
                    keyRead = raf.readInt();
                    if (keyRead == key) {

                        raf.skipBytes(62);
                        for (int i = 0; i < 5; i++) {
                            raf.writeInt(cartProductsRRN[i]);
                        }
                        return;
                    }
                    raf.skipBytes(82);
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class UserExtended extends User {

    // Strings com no máximo 60 bytes

    private String storeName;
    private String storeCategory;

    public UserExtended(int key, String password, String storeName, String storeCategory) {

        super(key, password);
        this.storeName = storeName;
        this.storeCategory = storeCategory;

    }

    public UserExtended(int key, String password) {

        super(key, password);
        this.storeName = "NILL";
        this.storeCategory = "NILL";

    }

    // Getters e Setters:
    public String getStoreName() {
        return storeName;
    }

    public String getStoreCategory() {
        return storeCategory;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setStoreCategory(String storeCategory) {
        this.storeCategory = storeCategory;
    }

    // Demais Métodos:
    private void exibirMinhaLoja() {
        MarketPlace.exibirLoja(key);

        System.out.print("\nUse addproduct para adicionar um produto\nUse remproduct para remover um produto\nUse editproduct para editar um produto\n[1] Voltar\n");
        Scanner input = new Scanner(System.in);

        while (true) {
            String comando = input.next();
            if (comando.equals("addproduct")) {
                int key = input.nextInt(); 
                MarketPlace.exibirLoja(key);
            } else if (comando.equals("1")) {
                break;
            }
        }
        this.painelInterativo();
    }

    @Override
    public void painelInterativo() {
        System.out.print(
                "MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n[3] Meu Carrinho\n[4] Minha Loja\n[5] Sair\n");
        Scanner input = new Scanner(System.in);

        int func = input.nextInt();

        if (func < 1 || func > 5) {
            System.out.println("Funcionalidade Inválida!");
            this.painelInterativo();
            return;
        }

        switch (func) {

            case 1:
                exibirCatalogo();
                break;

            case 2:
                exibirLojas();
                break;

            case 3:
                this.exibirCarrinho();
                break;

            case 4:
                this.exibirMinhaLoja();
                break;

            case 5:
                userDataSaving();
                System.exit(0);
        }
    }

    @Override
    public void newUserDataSaving() {

        try (RandomAccessFile raf = new RandomAccessFile("Users.bin", "rw")) {

            raf.seek(raf.length());
            raf.writeInt(key);
            MarketPlace.escreverString(raf, password);
            raf.writeChar('1');

            for (int i = 0; i < 5; i++) {
                raf.writeInt(cartProductsRRN[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try (RandomAccessFile raf = new RandomAccessFile("Stores.bin", "rw")) {

            raf.seek(raf.length());
            raf.writeInt(key);
            MarketPlace.escreverString(raf, storeName);
            MarketPlace.escreverString(raf, storeCategory);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
