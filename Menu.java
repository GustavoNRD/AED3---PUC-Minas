import java.util.Scanner;

public class Menu {
     public static void menuzin() {
    System.out.println("====== Menu ======");
    System.out.println("1. Criar um livro");
    System.out.println("2. Deletar um livro");
    System.out.println("3. Atualizar um livro");
    System.out.println("4. Ler um livro");
    System.out.println("5. Reorganizar");
    System.out.println("-1. Sair");
    System.out.println("==================");
  }

  // Método para executar as ações do menu
  public static void executarMenu(ArquivoLivros arqLivros) throws Exception{
    Scanner scanner = new Scanner(System.in);
    int opcao;
    
    do {
      menuzin();
      System.out.print("Escolha uma opção: ");
      opcao = scanner.nextInt();

      switch (opcao) {
        case 1:
          Livro novoLivro = criaLivro(scanner);
          arqLivros.create(novoLivro);
          System.out.println("Livro criado com sucesso.");
          break;
        case 2:
          deletaLivro(scanner, arqLivros);
          break;
        case 3:
          atualizarLivro(scanner, arqLivros);
          break;
        case 4:
          lerLivro(scanner, arqLivros);
          break;
        case 5:
          arqLivros.reorganizar();
          System.out.println("Arquivo de livros organizado!");
          break;
        case -1:
          System.out.println("Saindo...");
          break;
        default:
          System.out.println("Opção inválida.");
          break;
      }
    } while (opcao != -1);
  }

  static Livro criaLivro(Scanner scanner) {
    System.out.println("====== Criar Livro ======");
    String isbn;
    //pega um isbn valido
    do{
      System.out.print("(13 digitos) ISBN: ");
      isbn = scanner.next();
      scanner.nextLine();
   }while(isbn.length() != 13);
    
    System.out.print("Nome do livro: ");
    String nome = scanner.nextLine();
    float preco = 0.0f;
    boolean validInput = false;
    //pega um preço valido
    while (!validInput) {
        System.out.print("Preço: ");
        String precoStr = scanner.next();
        try {
            preco = Float.parseFloat(precoStr);
            validInput = true;
        } catch (NumberFormatException e) {
            System.out.println("Por favor, insira um preço válido.");
        }
    }

    Livro novoLivro = new Livro(-1, isbn, nome, preco);
    return novoLivro;
  }

  static void deletaLivro(Scanner scanner, ArquivoLivros arqLivros) throws Exception {
    System.out.println("====== Deletar Livro ======");
    System.out.print("Digite o ISBN (13 digitos) do livro a ser deletado: ");
    String isbn = scanner.next();
    
    try{
      Livro l1;
      l1 = arqLivros.readISBN(isbn);
      int id = l1.getID();
      if(arqLivros.delete(id))
        System.out.println("Livro deletado com sucesso.");
      else 
        System.out.println("Falha ao deletar, tente novamente.");
    }catch(Exception e){
      System.out.println("O ISBN digitado nao foi encontrado: " + isbn + " !\n");
    }
    
    
  }
  static void atualizarLivro(Scanner scanner, ArquivoLivros arqLivros) throws Exception {
    System.out.println("====== Atualizar Livro ======");
    System.out.print("Digite o ISBN do livro a ser atualizado: ");
    String isbn = scanner.next();
    scanner.nextLine();

    try{
      Livro antigo = arqLivros.readISBN(isbn);

      System.out.print("Digite o novo título (deixe em branco para manter o mesmo): ");
      String novoTitulo = scanner.nextLine();
      if (novoTitulo.isEmpty()) {
          novoTitulo = antigo.getTitulo();
      }

      System.out.print("Digite o novo preço (deixe em branco para manter o mesmo): ");
      String novoPrecoInput = scanner.nextLine();
      float novoPreco;
      if (novoPrecoInput.isEmpty()) {
          novoPreco = antigo.getPreco();
      } else {
          novoPreco = Float.parseFloat(novoPrecoInput);
      }

      Livro novo = new Livro(antigo.getID(), isbn, novoTitulo, novoPreco);
      arqLivros.update(novo);
    }catch(Exception e){
      System.out.println("O ISBN digitado nao foi encontrado: " + isbn + " !\n");
    }
  }

  static void lerLivro(Scanner scanner, ArquivoLivros arqLivros){
    System.out.println("====== Ver Livro ======");
    System.out.print("Digite o ISBN do livro a desejado: ");
    String isbn = scanner.next();
    scanner.nextLine();
    try{
      Livro livro = arqLivros.readISBN(isbn);
      System.out.println(livro);
      
    }catch(Exception e){
      System.out.println("O ISBN digitado nao foi encontrado: " + isbn + " !\n");
    }
  }

}
