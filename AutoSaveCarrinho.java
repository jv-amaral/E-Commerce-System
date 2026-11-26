import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** Uma sessão usa um único executor para serializar as gravações do carrinho. */
final class AutoSaveCarrinho {
    private final User usuario;
    private final Consumer<String> mostrarEstado;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private long versao;
    private long versaoSalva;
    private boolean encerrado;

    AutoSaveCarrinho(User usuario, Consumer<String> mostrarEstado) {
        this.usuario = usuario;
        this.mostrarEstado = mostrarEstado;
        executor.scheduleWithFixedDelay(this::salvarPeriodicamente, 10, 10, TimeUnit.SECONDS);
    }

    // A mudança de versão e a alteração do vetor ocorrem sob o mesmo bloqueio.
    synchronized void adicionar(int id) throws IOException {
        usuario.adicionarEmMemoria(id);
        versao++;
    }

    synchronized void remover(int posicao) {
        usuario.removerEmMemoria(posicao);
        versao++;
    }

    synchronized int[] itens() {
        return usuario.cartProductsRRN.clone();
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
        usuario.salvarCarrinho(copia);
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        synchronized (this) {
            versaoSalva = versaoDaCopia;
        }
        // Só anuncia a gravação se a tela ainda estiver mostrando a versão salva.
        SwingUtilities.invokeLater(() -> {
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
            SwingUtilities.invokeLater(() -> {
                if (!encerrado) {
                    mostrarEstado.accept("Alterações pendentes — falha ao salvar: " + e.getMessage());
                }
            });
        }
    }

    // A tela bloqueia novas ações até receber o resultado da última gravação.
    void encerrar(Runnable aoSair, Consumer<String> aoFalhar) {
        executor.execute(() -> {
            try {
                salvarPendente();
                executor.shutdown();
                SwingUtilities.invokeLater(() -> {
                    encerrado = true;
                    aoSair.run();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> aoFalhar.accept("Alterações pendentes — " + e.getMessage()));
            }
        });
    }
}
