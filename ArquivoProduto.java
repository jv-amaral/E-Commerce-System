import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ArquivoProduto {

    private static final String NOME_ARQUIVO = "Products.bin";
    private static final int TAMANHO_INDICE_DE_REMOCAO = 1;
    private static final int TAMANHO_STRING = 60;
    private static final int TAMANHO_DESCRICAO = 100;
    private static final int TAMANHO_CABECALHO = 8;

    // flag + idUsuario + idProduto + preco + estoque + nome + descricao
    private static final int TAMANHO_REGISTRO = TAMANHO_INDICE_DE_REMOCAO + 2 * Integer.BYTES
            + TAMANHO_STRING + Double.BYTES + Integer.BYTES + TAMANHO_DESCRICAO;

    private static final byte FLAG_ATIVO = 0;
    private static final byte FLAG_REMOVIDO = 1;

    // prepacao do arquivo
    private void prepararArquivo(RandomAccessFile arquivo) throws IOException {

        if (arquivo.length() == 0) {// se o arquivo estiver vazio
            arquivo.seek(0); // cursor é iniciado no byte 0 do cabecalho
            arquivo.writeInt(1); // proximo id de produto disponivel
            arquivo.writeInt(-1); // inicio da futura pilha
        }

        // Confere se existem um cabecalho completo e registros completos
        if (arquivo.length() < TAMANHO_CABECALHO
                || (arquivo.length() - TAMANHO_CABECALHO) % TAMANHO_REGISTRO != 0) {
            throw new IOException("O arquivo não corresponde ao formato de registros de 181 bytes.");
        }

        // volta para o incio do arquivo
        arquivo.seek(0);
        int proximoIdProduto = arquivo.readInt();
        int topoPilha = arquivo.readInt();

        // condicao da pilha permanecera -1 nessa primeira entrega
        // para um proxixa, a pilha sera iniciada
        if (proximoIdProduto <= 0 || topoPilha != -1) {
            throw new IOException("Cabeçalho inválido ou pilha de remoções ainda não suportada.");
        }
    }

    // transformar string em campo fixo com tratamento de erro
    private byte[] prepararTexto(String texto, int tamanho) {
        // recebe uma string com o seu tamanho para ransformar em um vetor de bytes
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O texto não pode estar vazio.");
        }

        // texto e codificado para UTF-8, e eh guardado no vetor de bytes
        byte[] bytesTexto = texto.getBytes(StandardCharsets.UTF_8);

        // se o texto codificado for maior que o espaco definido, o erro sera tratado
        if (bytesTexto.length > tamanho || texto.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "O texto deve caber em " + tamanho + " bytes e não conter caractere nulo.");
        }

        // cria o campo fixo, inicialmente preenchido com zeros
        byte[] campo = new byte[tamanho];

        // copia o conteúdo do texto para o campo e o resto fica com 0
        for (int i = 0; i < bytesTexto.length; i++) {
            campo[i] = bytesTexto[i];
        }
        return campo;
    }

    // metodo usado para ler um campo, comecando a leitura na posicao atual
    private String lerTexto(RandomAccessFile arquivo, int tamanho) throws IOException {
        byte[] campo = new byte[tamanho];
        arquivo.readFully(campo);
        int tamanhoReal = 0;
        // descobre o quanto do vetor eh realmente texto
        while (tamanhoReal < tamanho && campo[tamanhoReal] != 0) {
            tamanhoReal++;
        }
        return new String(campo, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    // vai ler apenas um byte, que vai ver se esta removido ou nao
    private byte lerFlag(RandomAccessFile arquivo) throws IOException {
        byte flag = arquivo.readByte();
        if (flag != FLAG_ATIVO && flag != FLAG_REMOVIDO) {
            throw new IOException("Flag de remoção inválida.");
        }
        return flag;
    }

    // metodo que ira escrever os registros
    private void escreverRegistro(RandomAccessFile arquivo, Produto produto) throws IOException {
        if (produto.getRemovido() != FLAG_ATIVO) {
            throw new IllegalArgumentException("A remoção será implementada na próxima entrega.");
        }

        // Valida todos os textos antes de escrever qualquer campo do registro
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

    // Retorna null para um registro marcado como removido, ou seja, byte = 1
    private Produto lerRegistro(RandomAccessFile arquivo) throws IOException {
        long inicio = arquivo.getFilePointer(); // sera utilizado para ser a posicao atual no arquivo
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

        // try eh usado para verificar possivel erro ou nao
        try {
            return new Produto(flag, nome, descricao, preco, quantidade, idProduto, idUsuario);
        } catch (IllegalArgumentException erro) {
            throw new IOException("O registro contém dados de produto inválidos.", erro);
        }
    }

    private long localizarRegistro(RandomAccessFile arquivo, int idProduto) throws IOException {
        if (idProduto <= 0) {
            throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
        }

        for (long posicao = TAMANHO_CABECALHO; posicao < arquivo.length(); posicao += TAMANHO_REGISTRO) {
            arquivo.seek(posicao);
            byte flag = lerFlag(arquivo);

            arquivo.skipBytes(Integer.BYTES);
            int idLido = arquivo.readInt();
            if (idLido == idProduto) {
                return posicao;
            }
        }
        throw new IllegalArgumentException("Produto não encontrado: " + idProduto);
    }

    // Gera o ID, grava o produto e devolve o objeto cadastrado
    public Produto cadastrar(String nome, String descricao, double preco,
            int quantidadeEstoque, int idLoja) throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            arquivo.seek(0);

            int idGerado = arquivo.readInt();

            //verifica a quantidade de produtos
            if (idGerado == Integer.MAX_VALUE) {
                throw new IllegalStateException("O contador de IDs atingiu o limite.");
            }

            Produto produto = new Produto(
                    FLAG_ATIVO, nome, descricao, preco, quantidadeEstoque, idGerado, idLoja);

            arquivo.seek(arquivo.length());
            escreverRegistro(arquivo, produto);
            arquivo.seek(0);
            arquivo.writeInt(idGerado + 1);
            return produto;
        }
    }

    public Produto buscarPorId(int id) throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            long posicao = localizarRegistro(arquivo, id);
            arquivo.seek(posicao);
            return lerRegistro(arquivo);
        }
    }

    public List<Produto> listarTodos() throws IOException {
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

    public List<Produto> listarPorLoja(int idLoja) throws IOException {
        if (idLoja <= 0) {
            throw new IllegalArgumentException("O ID da loja deve ser maior que zero.");
        }
        List<Produto> produtosDaLoja = new ArrayList<>();
        for (Produto produto : listarTodos()) {
            if (produto.getIdLoja() == idLoja) {
                produtosDaLoja.add(produto);
            }
        }
        return produtosDaLoja;
    }

    // Salva alteracoes feitas nos dados de um produto existente
    public void atualizar(Produto produto) throws IOException {
        if (produto == null) {
            throw new IllegalArgumentException("O produto não pode ser nulo.");
        }
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);
            long posicao = localizarRegistro(arquivo, produto.getIdProduto());
            arquivo.seek(posicao);
            Produto original = lerRegistro(arquivo);

            if (original.getIdLoja() != produto.getIdLoja()) {
                throw new IllegalArgumentException("Não é possível trocar a loja do produto.");
            }

            arquivo.seek(posicao);
            escreverRegistro(arquivo, produto);
        }
    }
}
