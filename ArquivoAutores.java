import aeds3.Arquivo;

public class ArquivoAutores extends Arquivo<Autor> {

  public ArquivoAutores() throws Exception {
    super("autores", Autor.class.getConstructor());
  }

}
