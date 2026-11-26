package controller;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import model.ArquivoProduto;
import model.ArquivoUsuario;
import model.User;

/** Uma sessão usa um único executor para serializar as gravações periódicas do carrinho. */
public final class AutoSaveCarrinho {
    private final User usuario;
    private final Consumer<String> mostrarEstado;
    private final Executor despachanteInterface;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private long versao;
    private long versaoSalva;
    private boolean encerrado;

    public AutoSaveCarrinho(User usuario, Consumer<String> mostrarEstado, Executor despachanteInterface) {
        this.usuario = usuario;
        this.mostrarEstado = mostrarEstado;
        this.despachanteInterface = despachanteInterface;
        executor.scheduleWithFixedDelay(this::salvarPeriodicamente, 10, 10, TimeUnit.SECONDS);
    }

    // A mudança de versão e a alteração do vetor ocorrem sob o mesmo bloqueio.
    public synchronized void adicionar(int idProduto) throws IOException {
        int rrn = ArquivoProduto.rrnDisponivel(idProduto);
        usuario.adicionarRrnAoCarrinho(rrn);
        versao++;
    }

    public synchronized void remover(int posicao) {
        usuario.removerPosicaoDoCarrinho(posicao);
        versao++;
    }

    public synchronized int[] itens() {
        return usuario.getCartProductsRRN();
    }

    private void salvarPendente() throws IOException {
        final long versaoDaCopia;
        final int[] copia;
        synchronized (this) {
            if (versao == versaoSalva) {
                return;
            }
            versaoDaCopia = versao;
            copia = itens();
        }
        // A escrita usa a cópia fora do bloqueio, permitindo novas edições na interface.
        ArquivoUsuario.salvarCarrinho(usuario.getKey(), copia);
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        synchronized (this) {
            versaoSalva = versaoDaCopia;
        }
        // Só anuncia a gravação se a interface ainda estiver exibindo a versão salva.
        despachanteInterface.execute(() -> {
            synchronized (this) {
                if (!encerrado && versao == versaoDaCopia) {
                    mostrarEstado.accept("Salvo às " + hora);
                }
            }
        });
    }

    private void salvarPeriodicamente() {
        try {
            salvarPendente();
        } catch (IOException e) {
            despachanteInterface.execute(() -> {
                if (!encerrado) {
                    mostrarEstado.accept("Alterações pendentes — falha ao salvar: " + e.getMessage());
                }
            });
        }
    }

    // A interface bloqueia novas ações até receber o resultado da última gravação.
    public void encerrar(Runnable aoSair, Consumer<String> aoFalhar) {
        executor.execute(() -> {
            try {
                salvarPendente();
                executor.shutdown();
                despachanteInterface.execute(() -> {
                    encerrado = true;
                    aoSair.run();
                });
            } catch (IOException e) {
                despachanteInterface.execute(() -> aoFalhar.accept("Alterações pendentes — " + e.getMessage()));
            }
        });
    }
}
