package model;

/** Modelo de usuário cliente do sistema de marketplace. */
public class User {
    protected int key;
    protected String password;
    protected int[] cartProductsRRN;

    public User(int key, String password) {
        setKey(key);
        setPassword(password);
        cartProductsRRN = new int[] { -1, -1, -1, -1, -1 };
    }

    public void setKey(int key) {
        ArquivoBinario.validarChave(key);
        this.key = key;
    }

    public void setPassword(String password) {
        ArquivoBinario.validarTexto(password, ArquivoBinario.TAMANHO_STRING, "Senha");
        this.password = password;
    }

    public void setCartProductsRRN(int RRN, int pos) {
        if (pos < 0 || pos >= cartProductsRRN.length || RRN < -1) {
            throw new IllegalArgumentException("Posição ou RRN do carrinho inválido.");
        }
        this.cartProductsRRN[pos] = RRN;
    }

    public int getKey() {
        return key;
    }

    public String getPassword() {
        return password;
    }

    public int getCartProductRRN(int pos) {
        if (pos < 0 || pos >= cartProductsRRN.length) {
            throw new IllegalArgumentException("Posição do carrinho inválida.");
        }
        return cartProductsRRN[pos];
    }

    public int[] getCartProductsRRN() {
        return cartProductsRRN.clone();
    }

    public int getCapacidadeCarrinho() {
        return cartProductsRRN.length;
    }

    public boolean isCarrinhoVazio() {
        for (int rrn : cartProductsRRN) {
            if (rrn != -1) {
                return false;
            }
        }
        return true;
    }

    public int adicionarRrnAoCarrinho(int rrn) {
        if (rrn < 0) {
            throw new IllegalArgumentException("RRN do carrinho inválido.");
        }
        for (int i = 0; i < cartProductsRRN.length; i++) {
            if (cartProductsRRN[i] == -1) {
                cartProductsRRN[i] = rrn;
                return i;
            }
        }
        throw new IllegalArgumentException("Seu carrinho está cheio!");
    }

    public void removerPosicaoDoCarrinho(int posicao) {
        if (posicao < 0 || posicao >= cartProductsRRN.length || cartProductsRRN[posicao] == -1) {
            throw new IllegalArgumentException("Selecione um item do carrinho.");
        }
        setCartProductsRRN(-1, posicao);
    }

    public boolean isLojista() {
        return false;
    }

    public char getTipoRegistro() {
        return '0';
    }

    public void validarDados() {
        ArquivoBinario.validarChave(key);
        ArquivoBinario.validarTexto(password, ArquivoBinario.TAMANHO_STRING, "Senha");
        if (cartProductsRRN == null || cartProductsRRN.length != ArquivoBinario.TAMANHO_CARRINHO) {
            throw new IllegalArgumentException("O carrinho deve possuir cinco posições.");
        }
        for (int rrn : cartProductsRRN) {
            if (rrn < -1) {
                throw new IllegalArgumentException("RRN do carrinho inválido.");
            }
        }
    }
}
