//fazer classes?

public class Usuario{
    private int papel; //0 para usuario padrao, 1 para vendedor
    private String criar_senha;
    private String email;
    private String data_de_nascimento;
    private String nome;
    private String sobrenome;
    //crie alguns atributos que acho que são relevantes para o sistema

    //iniciar um construtor
    public Usuario(int papel, String criar_senha, String email, String data_de_nascimento, String nome, String sobrenome)
    {
        if(nome == null)
        {
            throw new IllegalArgumentException("Por Favor, insira um nome valido");
        }
        if(sobrenone==null)
        {
            throw new IllegalArgumentException("Por Favor, insira um sobrenome valido");
        }
        //vou arrumar isso

    }
}