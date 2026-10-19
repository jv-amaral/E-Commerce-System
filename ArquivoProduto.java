

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
    private static final int TAMANHO_REGISTRO = 181;

    // prepacao do arquivo
    private void prepararArquivo(RandomAccessFile arquivo)
            throws IOException {
        if (arquivo.length() == 0) {// se o arquivo estiver vazio
            arquivo.seek(0); // cursor é iniciado no byte 0 do cabecalho
            arquivo.writeInt(1); // proximo id de produto disponivel
            arquivo.writeInt(-1); // inicio da futura pilha
        }

        // Confere se existem um cabecalho completo e registros completos
        if (arquivo.length() < TAMANHO_CABECALHO || (arquivo.length() - TAMANHO_CABECALHO) % TAMANHO_REGISTRO != 0) {
            throw new IOException("O arquivo de produtos tem formato inválido.");
        }

        // volta para o incio do arquivo
        arquivo.seek(0);

        int proximoIdProduto = arquivo.readInt();
        int topoPilha = arquivo.readInt();

        // condicao da pilha permanecera -1 nessa primeira entrega
        // para um proxixa, a pilha sera iniciada
        if (proximoIdProduto <= 0 || topoPilha != -1) {
            throw new IOException("Cabeçalho inválido ou remoções ainda não suportadas.");
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

            
        if (bytesTexto.length > tamanho || texto.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "O texto deve caber em " + tamanho + " bytes e não conter caractere nulo.");
        }


        // cria o campo fixo, inicialmente preenchido com zeros
        byte[] campo = new byte[tamanho];

        // copia o conteúdo do texto para o campo
        for (int i = 0; i < bytesTexto.length; i++) {
            campo[i] = bytesTexto[i];
        }

        return campo;
    }

    private String lerTexto(RandomAccessFile arquivo, int tamanho) throws IOException {
        byte[] campo = new byte[tamanho];
        arquivo.readFully(campo);

        int tamanhoReal = tamanho;
        while (tamanhoReal > 0 && campo[tamanhoReal - 1] == 0) {
            tamanhoReal--;
        }

        return new String(campo, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    private void escreverRegistro(RandomAccessFile arquivo, Produto produto)
            throws IOException {
        // Valida todos os textos antes de escrever qualquer campo do registro
        byte[] nome = prepararTexto(produto.getNome(), TAMANHO_STRING);
        byte[] categoria = prepararTexto(produto.getCategoria(), TAMANHO_STRING);
        byte[] descricao = prepararTexto(produto.getDescricao(), TAMANHO_DESCRICAO);

        arquivo.writeInt(produto.getId());
        arquivo.writeInt(produto.getIdLoja());
        arquivo.write(nome);
        arquivo.write(categoria);
        arquivo.write(descricao);
        arquivo.writeDouble(produto.getPreco());
        arquivo.writeInt(produto.getQuantidade());
    }

    private Produto lerRegistro(RandomAccessFile arquivo) throws IOException {
        int id = arquivo.readInt();
        int idLoja = arquivo.readInt();
        String nome = lerTexto(arquivo, TAMANHO_STRING);
        String categoria = lerTexto(arquivo, TAMANHO_STRING);
        String descricao = lerTexto(arquivo, TAMANHO_DESCRICAO);
        double preco = arquivo.readDouble();
        int quantidade = arquivo.readInt();

        try {
            return new Produto(nome, categoria, descricao, preco, quantidade, id, idLoja);
        } catch (IllegalArgumentException erro) {
            throw new IOException("O registro contém dados de produto inválidos.", erro);
        }
    }

    private long localizarRegistro(RandomAccessFile arquivo, int id) throws IOException {
        if (id <= 0) {
            throw new IllegalArgumentException("O ID deve ser maior que zero.");
        }

        for (long posicao = TAMANHO_CABECALHO; posicao < arquivo.length(); posicao += TAMANHO_REGISTRO) {
            arquivo.seek(posicao);
            if (arquivo.readInt() == id) {
                return posicao;
            }
        }

        throw new IllegalArgumentException("Produto não encontrado: " + id);
    }

    // Gera o ID, grava o produto e devolve o objeto cadastrado
    public Produto cadastrar(String nome, String categoria, String descricao,
            double preco, int quantidadeEstoque, int idLoja)
            throws IOException {
        try (RandomAccessFile arquivo = new RandomAccessFile(NOME_ARQUIVO, "rw")) {
            prepararArquivo(arquivo);

            arquivo.seek(0);
            int idGerado = arquivo.readInt();

            if (idGerado == Integer.MAX_VALUE) {
                throw new IllegalStateException("O contador de IDs atingiu o limite.");
            }

            Produto produto = new Produto(
                    nome, categoria, descricao, preco, quantidadeEstoque, idGerado, idLoja);

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
                produtos.add(lerRegistro(arquivo));
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
            long posicao = localizarRegistro(arquivo, produto.getId());

            arquivo.seek(posicao + Integer.BYTES);
            int idLojaOriginal = arquivo.readInt();
            if (idLojaOriginal != produto.getIdLoja()) {
                throw new IllegalArgumentException("Não é possível transferir o produto de loja.");
            }

            arquivo.seek(posicao);
            escreverRegistro(arquivo, produto);
        }
    }
}
