import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

class SenhaIncorretaException extends Exception {
    public SenhaIncorretaException(String mensagem) {
        super(mensagem);
    }
}

class DadosNaoEncontradosException extends Exception {
    public DadosNaoEncontradosException(String mensagem) {
        super(mensagem);
    }
}

public class MarketPlace {
    
    private static int TAMANHO_STRING = 20; 
    private static int TAM_REG_USUARIO = 46;
    
    // Métodos estáticos principais: 
    private static User readUsersData(int key, String password) throws IOException, SenhaIncorretaException, DadosNaoEncontradosException{

        File arq = new File("Users.bin");
        int keyRead, RRNread; 
        String passwordRead; //senha a ser lida no arquivo 
        char userType; 
        User user; 

        try (RandomAccessFile raf = new RandomAccessFile(arq,"rw")){

            while (true){
                try{
                    keyRead = raf.readInt(); 

                    if (keyRead == key){ 

                        passwordRead = lerString(raf); 

                        if (!passwordRead.equals(password)){
                            throw new SenhaIncorretaException("Senha incorreta!");
                        }

                        userType = raf.readChar(); 
                        if (userType == '0'){
                            user = new User(key, password);
                        } else {
                            user = new UserExtended(key, password);
                        }

                        for (int i = 0; i < 5; i++){
                            RRNread = raf.readInt(); 
                            user.setCartProductsRRN(RRNread, i);
                        }

                        return user; 
                    }
                    raf.skipBytes(42); // pula para o próximo registro

                } catch(EOFException e){
                    break;
                }
            }
            throw new DadosNaoEncontradosException("Usuario nao cadastrado!");
        }
    }

    public static String[] readStoreNameAndCategory(int key) throws IOException, DadosNaoEncontradosException{

        int keyRead; 
        String[] result = new String[2]; 

        try (RandomAccessFile raf = new RandomAccessFile("Store.bin", "rw")) {

            while(true){
                try {
                    keyRead = raf.readInt();

                    if (keyRead == key){

                        result[0] = lerString(raf);
                        result[1] = lerString(raf); 
                        return result;

                    }

                } catch (EOFException e){
                    break;
                }
            }
            throw new DadosNaoEncontradosException("Loja não cadastrada!");
        }

    }

    // Métos estáticos auxiliares:
    public static void escreverString(RandomAccessFile raf, String texto) throws IOException {

        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        byte[] bufferFixo = new byte[TAMANHO_STRING];

        // Copia os bytes da string truncando se ultrapassar o limite fixo
        int bytesParaCopiar = Math.min(bytesOriginal.length, TAMANHO_STRING);
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesParaCopiar);

        raf.write(bufferFixo);
    }

    public static String lerString(RandomAccessFile raf) throws IOException {

        byte[] buffer = new byte[TAMANHO_STRING];
        raf.readFully(buffer); // Garante a leitura de exatamente TAMANHO_STRING_BYTES
        
        // Converte e remove os bytes nulos/espaços de preenchimento
        return new String(buffer, StandardCharsets.UTF_8).trim();
    }



}
