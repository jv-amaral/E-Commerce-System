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

    // Strings com no máximo 20 chars 

    private int key;  
    private String password; 
    private int[] cartProductsRRN; 


    // Construtores: 
    public User(int key, String password){
        // utilizado para criar um usuário novo no sistema 
        this.key = key; 
        this.password = password; 
        cartProductsRRN = new int[]{-1,-1,-1,-1,-1};
    }

    public User(int key){
        this.key = key; 
    }

    // Setters e Getters: 
    public void setKey(int key){
        this.key = key;
    }

    public void setPassword(String password){
        this.password = password; 
    }

    public void setCartProductsRRN(int RRN, int pos){
        this.cartProductsRRN[pos] = RRN;
    }

    public int getKey(){
        return key; 
    }

    public String getPassword(){
        return password; 
    } 

    // Demais métodos: 
    static protected void exibirCatalogo(){


        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "rw")) {

            while(true){
                try {
                    




                    


                } catch (EOFException e){
                    break;
                }







            }
    
  
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static protected void exibirLojas(){

    }

    protected void exibirCarrinho(){

    }

    public void painelInterativo() throws FuncionalidadeInvalidaException{

        System.out.print("MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n[3] Meu Carrinho\n[4] Sair\n");
        Scanner input = new Scanner(System.in); 

        int func = input.nextInt();

        if (func < 1 || func > 4){
            throw new FuncionalidadeInvalidaException("Funcionalidade invalida!");
        }

        switch (func){

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
                System.exit(0);
        }


















    }


}

class UserExtended extends User {

    // Strings com no máximo 20 chars 

    private String storeName; 
    private String storeCategory; 

    public UserExtended(int key, String password, String storeName, String storeCategory){

        super(key, password); 
        this.storeName = storeName; 
        this.storeCategory = storeCategory; 

    }

    public UserExtended(int key, String password){

        super(key, password); 
        this.storeName = "NILL"; 
        this.storeCategory = "NILL";

    }

    // Getters e Setters: 
    public String getStoreName(){
        return storeName; 
    }

    public String getStoreCategory(){
        return storeCategory;
    }

    public void setStoreName(String storeName){
        this.storeName = storeName; 
    }

    public void setStoreCategory(String storeCategory){
        this.storeCategory = storeCategory; 
    }

    // Demais Métodos: 
    private void exibirMinhaLoja(){


    } 

    @Override 
    public void painelInterativo() throws FuncionalidadeInvalidaException{
        System.out.print("MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n[3] Meu Carrinho\n[4] Minha Loja\n[5] Sair");
        Scanner input = new Scanner(System.in); 

        int func = input.nextInt();

        if (func < 1 || func > 5){
            throw new FuncionalidadeInvalidaException("Funcionalidade invalida!");
        }

        switch (func){

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
                System.exit(0);
        }
    }



    
}