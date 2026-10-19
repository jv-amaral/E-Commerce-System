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
    
    private static int TAMANHO_STRING = 60; 
    //private static int TAM_REG_USUARIO = 
    
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
                    raf.skipBytes(82); // pula para o próximo registro

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

        try (RandomAccessFile raf = new RandomAccessFile("Stores.bin", "rw")) {

            while(true){
                try {
                    keyRead = raf.readInt();

                    if (keyRead == key){

                        result[0] = lerString(raf);
                        result[1] = lerString(raf); 
                        return result;

                    }   
                    raf.skipBytes(120);


                } catch (EOFException e){
                    break;
                }
            }
            throw new DadosNaoEncontradosException("Loja não cadastrada!");
        }

    }

    public static void exibirLoja(int key){

        int keyRead;
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
                    keyRead = raf.readInt();
                    id = raf.readInt();
                    preco = raf.readDouble();
                    estoque = raf.readInt();
                    nome = MarketPlace.lerString(raf);
                    descricao = MarketPlace.lerString(raf, 100);

                    if (removido == 1 || key != keyRead) {
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
    }

    // Métos estáticos auxiliares:
    public static void escreverString(RandomAccessFile raf, String texto) throws IOException {
        if (texto == null) {
            texto = "";
        }

        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        if (bytesOriginal.length > TAMANHO_STRING) {
            throw new IllegalArgumentException("O texto excede o limite máximo de " + TAMANHO_STRING + " bytes!");
        }

        byte[] bufferFixo = new byte[TAMANHO_STRING];
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesOriginal.length);

        raf.write(bufferFixo);
    }

    public static String lerString(RandomAccessFile raf) throws IOException {
        byte[] buffer = new byte[TAMANHO_STRING];
        raf.readFully(buffer); // Garante a leitura de exatamente TAMANHO_STRING bytes
        
        // Determina o tamanho real da string até o primeiro byte nulo de preenchimento
        int tamanhoReal = 0;
        while (tamanhoReal < buffer.length && buffer[tamanhoReal] != 0) {
            tamanhoReal++;
        }

        // Decodifica preservando eventuais espaços válidos e sem truncamento indevido
        return new String(buffer, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    public static void escreverString(RandomAccessFile raf, String texto, int tam_bytes) throws IOException {
        if (texto == null) {
            texto = "";
        }

        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        if (bytesOriginal.length > tam_bytes) {
            throw new IllegalArgumentException("O texto excede o limite máximo de " + tam_bytes + " bytes!");
        }

        byte[] bufferFixo = new byte[tam_bytes];
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesOriginal.length);

        raf.write(bufferFixo);
    }

    public static String lerString(RandomAccessFile raf, int tam_bytes) throws IOException {
        byte[] buffer = new byte[tam_bytes];
        raf.readFully(buffer); // Garante a leitura de exatamente TAMANHO_STRING bytes
        
        // Determina o tamanho real da string até o primeiro byte nulo de preenchimento
        int tamanhoReal = 0;
        while (tamanhoReal < buffer.length && buffer[tamanhoReal] != 0) {
            tamanhoReal++;
        }

        // Decodifica preservando eventuais espaços válidos e sem truncamento indevido
        return new String(buffer, 0, tamanhoReal, StandardCharsets.UTF_8);
    }
}
