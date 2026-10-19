package modelo;

//definicao da classe loja

public class Loja {
    private int id;
    private int idDono;
    private String nome;
    private String categoria;
}

public Loja(int id, int idDono, int nome, int categoria)
{
    if (id<=0)
    {
        throw IllegalArgumentException(
            "O identificador da loja deve ser maior que 0");
    }

    if(idDono<=0)
    {
        throw IllegalArgumentException(
            "O identificador do dono deve ser maior que 0";
        )
    }
    if(nome == null || nome.isBlank)
    {
        throw IllegalArgumentException(
            "O nome da loja não pode ser vazio");
    }

    if(categoria = null || categoria.isBlank)
    {
        throw IllegalArgumentException(
            "A categoria da loja não pode ser vazia");
    }
    this.id = id;
    this.idDono = idDono;
    this.nome = nome;
    this.categoria = categoria;

}


// implementacao dos metodos
// para essa primeira implementacao nao sera possivel alterar os parametros da loja
public int getId() {
    return id;
}

public int getIdDono() {
    return idDono;
}

public String getNome() {
    return nome;
}

public String getCategoria() {
    return categoria;
}