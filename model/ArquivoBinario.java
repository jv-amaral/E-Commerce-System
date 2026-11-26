package model;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/** Constantes de formato fixo e utilitários de validação e leitura/escrita binária. */
public final class ArquivoBinario {
    public static final int TAMANHO_STRING = 60;
    public static final int TAMANHO_DESCRICAO = 100;
    public static final int TAMANHO_CARRINHO = 5;
    public static final int TAM_REG_USUARIO = Integer.BYTES + TAMANHO_STRING
            + Character.BYTES + TAMANHO_CARRINHO * Integer.BYTES;
    public static final int TAM_REG_LOJA = Integer.BYTES + 2 * TAMANHO_STRING;
    public static final int TAM_CAB_PRODUTO = 8;
    public static final int TAM_REG_PRODUTO = 181;

    private ArquivoBinario() {
    }

    public static void validarChave(int key) {
        if (key <= 0) {
            throw new IllegalArgumentException("O NROUSP deve ser maior que zero.");
        }
    }

    public static void validarTexto(String texto, int tamanho, String campo) {
        if (texto == null || texto.isBlank() || texto.indexOf('\0') >= 0
                || texto.getBytes(StandardCharsets.UTF_8).length > tamanho) {
            throw new IllegalArgumentException(
                    campo + " deve conter texto válido de até " + tamanho + " bytes.");
        }
    }

    public static void escreverString(RandomAccessFile raf, String texto) throws IOException {
        escreverString(raf, texto, TAMANHO_STRING);
    }

    public static void escreverString(RandomAccessFile raf, String texto, int tamBytes)
            throws IOException {
        if (tamBytes <= 0) {
            throw new IllegalArgumentException("O tamanho do campo deve ser positivo.");
        }
        if (texto == null) {
            texto = "";
        }
        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        if (bytesOriginal.length > tamBytes || texto.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "O texto deve caber em " + tamBytes + " bytes e não conter caractere nulo.");
        }
        byte[] bufferFixo = new byte[tamBytes];
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesOriginal.length);
        raf.write(bufferFixo);
    }

    public static String lerString(RandomAccessFile raf) throws IOException {
        return lerString(raf, TAMANHO_STRING);
    }

    public static String lerString(RandomAccessFile raf, int tamBytes) throws IOException {
        if (tamBytes <= 0) {
            throw new IllegalArgumentException("O tamanho do campo deve ser positivo.");
        }
        byte[] buffer = new byte[tamBytes];
        raf.readFully(buffer);
        int tamanhoReal = 0;
        while (tamanhoReal < buffer.length && buffer[tamanhoReal] != 0) {
            tamanhoReal++;
        }
        return new String(buffer, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    public static void conferirRegistros(RandomAccessFile raf, int tamanho, String nome)
            throws IOException {
        if (raf.length() % tamanho != 0) {
            throw new IOException(nome + " contém um registro incompleto.");
        }
        raf.seek(0);
    }

    public static void conferirProdutos(RandomAccessFile raf) throws IOException {
        if (raf.length() < TAM_CAB_PRODUTO
                || (raf.length() - TAM_CAB_PRODUTO) % TAM_REG_PRODUTO != 0) {
            throw new IOException("Products.bin contém um cabeçalho ou registro incompleto.");
        }
    }

    public static void restaurarTamanho(RandomAccessFile raf, long tamanho, Throwable erro) {
        try {
            raf.setLength(tamanho);
        } catch (IOException falha) {
            erro.addSuppressed(falha);
        }
    }
}
