package model;

/** Modelo de usuário lojista do sistema de marketplace. */
public class UserExtended extends User {
    private String storeName;
    private String storeCategory;

    public UserExtended(int key, String password, String storeName, String storeCategory) {
        super(key, password);
        setStoreName(storeName);
        setStoreCategory(storeCategory);
    }

    public UserExtended(int key, String password) {
        this(key, password, "NILL", "NILL");
    }

    public String getStoreName() {
        return storeName;
    }

    public String getStoreCategory() {
        return storeCategory;
    }

    public void setStoreName(String storeName) {
        ArquivoBinario.validarTexto(storeName, ArquivoBinario.TAMANHO_STRING, "Nome da loja");
        this.storeName = storeName;
    }

    public void setStoreCategory(String storeCategory) {
        ArquivoBinario.validarTexto(storeCategory, ArquivoBinario.TAMANHO_STRING, "Categoria da loja");
        this.storeCategory = storeCategory;
    }

    @Override
    public boolean isLojista() {
        return true;
    }

    @Override
    public char getTipoRegistro() {
        return '1';
    }

    @Override
    public void validarDados() {
        super.validarDados();
        ArquivoBinario.validarTexto(storeName, ArquivoBinario.TAMANHO_STRING, "Nome da loja");
        ArquivoBinario.validarTexto(storeCategory, ArquivoBinario.TAMANHO_STRING, "Categoria da loja");
    }
}
