package aeds3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Arquivo<T extends Registro> {

  protected static int TAM_CABECALHO = 12;
  protected RandomAccessFile arquivo;
  protected HashExtensivel<ParIDEndereco> indiceDireto;
  private String nomeEntidade;
  private Constructor<T> construtor;

  public Arquivo(String na, Constructor<T> c) throws Exception {
    this.nomeEntidade = na;
    this.construtor = c;
    arquivo = new RandomAccessFile("dados/" + this.nomeEntidade + ".db", "rwd");
    if (arquivo.length() < TAM_CABECALHO) {
      arquivo.seek(0);
      arquivo.writeInt(0);
      arquivo.writeLong(0);
    }
    indiceDireto = new HashExtensivel<>(ParIDEndereco.class.getConstructor(),
        4, "dados/" + this.nomeEntidade + ".hash_d.db",
        "dados/" + this.nomeEntidade + ".hash_c.db");
  }

  public int create(T obj) throws Exception {
    int ultimoID;
    arquivo.seek(0);
    ultimoID = arquivo.readInt();
    ultimoID++;
    arquivo.seek(0);
    arquivo.writeInt(ultimoID);
    obj.setID(ultimoID);

    byte[] ba = obj.toByteArray();
    
    //arquivo.seek(arquivo.length()); //forma antiga apenas colocando no final do arquivo
    
    long endereco; //endereço do registro excluido
    long enderecoEndereco; //endereço do endereço do registro excluido

    arquivo.seek(4);
    enderecoEndereco = arquivo.getFilePointer(); //salvo o local onde foi lido o endereço
    endereco = arquivo.readLong(); //leio o primeiro endereço de exclusão no cabeçalho 
    while(endereco != 0)
    {
      arquivo.seek(endereco + 1); //pulo para o indicador de tamanho do registro
      short tamanho = arquivo.readShort(); //lê o indicador de tamanho

      if(ba.length <= tamanho) //caso o novo registro caiba 
      {
        long enderecoTmp = arquivo.readLong(); //pega o endereço do proximo registro excluido

        arquivo.write(ba); //escreve o novo registro
        indiceDireto.create(new ParIDEndereco(ultimoID, endereco)); //cria o indice 

        arquivo.seek(endereco); //volta para o lapide do registro não mais excluido
        arquivo.writeByte(' '); //limpa o lápide

        arquivo.seek(enderecoEndereco); // volta para o ponteiro anterior
        arquivo.writeLong(enderecoTmp); // escreve o endereço do proximo registro excluido

        return obj.getID(); //retorna o ID
      }
      else
      {
        arquivo.seek(endereco + 3); // pula o lápide e o indicador de tamanho
        enderecoEndereco = arquivo.getFilePointer(); // guarda o endereço do ponteiro 
        endereco = arquivo.readLong(); // lê o endereço para o proximo registro exlcuido
      }
    }

    arquivo.seek(arquivo.length());
    
    endereco = arquivo.getFilePointer();
    arquivo.writeByte(' ');
    arquivo.writeShort(ba.length);
    arquivo.write(ba);

    indiceDireto.create(new ParIDEndereco(ultimoID, endereco));
    return obj.getID();
  }

  public T read(int id) throws Exception 
  {
    T obj = construtor.newInstance();
    int tam;
    byte lapide;

    long endereco = indiceDireto.read(id).getEndereco();
    arquivo.seek(endereco);
    if (endereco > 0) {
      lapide = arquivo.readByte();
      tam = arquivo.readShort();
      byte[] ba = new byte[tam];
      arquivo.read(ba);
      if (lapide == ' ') {
        obj.fromByteArray(ba);
        if (obj.getID() == id)
          return obj;
      } else {
        return null;
      }
    }
    return null;
  }

  public boolean delete(int id) throws Exception {
    long endereco = indiceDireto.read(id).getEndereco();
    if (endereco > 0) {
      arquivo.seek(endereco);
      arquivo.writeByte('*');
      arquivo.seek(endereco + 3);
      arquivo.writeLong(0);
      indiceDireto.delete(id);

      long enderecoTmp; // endereço de registro excluido
      long enderecoEndereco; // local onde esta o endereço
      arquivo.seek(4);
      enderecoEndereco = arquivo.getFilePointer(); //guarda o local do primeiro ponteiro
      enderecoTmp = arquivo.readLong(); //lê o primeiro endereço

      while(enderecoTmp != 0) //se for 0 já não entra aqui
      {
        arquivo.seek(enderecoTmp + 3); // pula o lápide e o indicador de tamanho
        enderecoEndereco = arquivo.getFilePointer(); //guarda o local do ponteiro
        enderecoTmp = arquivo.readLong(); //lê o endereço
        if(enderecoTmp == 0) //se for 0 então 
        {
          arquivo.seek(enderecoEndereco); //volta para o endereço
          arquivo.writeLong(endereco); //escreve no ponteiro o local do novo registro excluido
          return true; //retorna true
        } //se não, continua o loop
      }

      arquivo.seek(enderecoEndereco); //volta para o ponteiro 0
      arquivo.writeLong(endereco); // escreve o endereço 

      return true;
    } else
      return false;
  }

  public boolean update(T objAtualizado) throws Exception {
    T obj = construtor.newInstance();
    int tam;
    byte lapide;
    long endereco = indiceDireto.read(objAtualizado.getID()).getEndereco();

    if (endereco > 0) {
      // Lê o registro atual
      arquivo.seek(endereco + 1); // pula o lápide
      tam = arquivo.readShort();
      byte[] ba = new byte[tam];
      arquivo.read(ba);
      obj.fromByteArray(ba);

      // determina se o registro cresceu ou náo
      byte[] ba2 = objAtualizado.toByteArray();
      short tam2 = (short) ba2.length;

      // novo registro permanece no mesmo lugar
      if (tam2 <= tam) {
        arquivo.seek(endereco + 3);
        arquivo.write(ba2);
        
      } 
      else 
      {
        delete(objAtualizado.getID()); // deleta o registro anterior 


        long enderecoEndereco;
        long enderecoTmp;
        arquivo.seek(4);
        enderecoEndereco = arquivo.getFilePointer(); //salvo o local onde foi lido o endereço
        enderecoTmp = arquivo.readLong(); //leio o primeiro endereço de exclusão no cabeçalho 
        while(enderecoTmp != 0)
        {
          arquivo.seek(enderecoTmp + 1); //pulo para o indicador de tamanho do registro
          short tamanho = arquivo.readShort(); //lê o indicador de tamanho
    
          if(ba2.length <= tamanho) //caso o novo registro caiba 
          {
            long enderecoTmp2 = arquivo.readLong(); //pega o endereço do proximo registro excluido
            arquivo.write(ba2); //escreve o novo registro
            indiceDireto.update(new ParIDEndereco(objAtualizado.getID(), enderecoTmp)); //atualiza o indice 

            arquivo.seek(enderecoTmp); //volta para o lapide do registro não mais excluido
            arquivo.writeByte(' '); //limpa o lápide
    
            arquivo.seek(enderecoEndereco); // volta para o ponteiro anterior
            arquivo.writeLong(enderecoTmp2); // escreve o endereço do proximo registro excluido
    
            return true;
          }
          else
          {
            arquivo.seek(endereco + 3); // pula o lápide e o indicador de tamanho
            enderecoEndereco = arquivo.getFilePointer(); // guarda o endereço do ponteiro 
            enderecoTmp = arquivo.readLong(); // lê o endereço para o proximo registro exlcuido
          }
        }


        arquivo.seek(arquivo.length());
        enderecoTmp = arquivo.getFilePointer();
        arquivo.writeByte(' ');
        arquivo.writeShort(ba2.length);
        arquivo.write(ba2);
        indiceDireto.create(new ParIDEndereco(objAtualizado.getID(), enderecoTmp));
      }



      return true;
    } else
      return false;
  }

  public void close() throws Exception {
    arquivo.close();
  }

  // REORGANIZAR - VERSÃO QUE REORDENA O ARQUIVO, USANDO INTERCALAÇÃO BALANCEADA
  // Recebe um objeto vazio para auxiliar na reorganização
  @SuppressWarnings("unchecked")
  public void reorganizar() throws Exception {

    // Lê o cabeçalho
    byte[] ba_cabecalho = new byte[TAM_CABECALHO];
    arquivo.seek(0);
    arquivo.read(ba_cabecalho);

    // ---------------------------------------------------------------------
    // Primeira etapa (distribuição)
    // ---------------------------------------------------------------------
    int tamanhoBlocoMemoria = 3;
    List<T> registrosOrdenados = new ArrayList<>();

    int contador = 0, seletor = 0;
    short tamanho;
    byte lapide;
    byte[] dados;
    T r1 = construtor.newInstance(),
        r2 = construtor.newInstance(),
        r3 = construtor.newInstance();
    T rAnt1, rAnt2, rAnt3;

    // Abre três arquivos temporários para escrita (1º conjunto)
    DataOutputStream out1 = new DataOutputStream(new FileOutputStream("dados/temp1.db"));
    DataOutputStream out2 = new DataOutputStream(new FileOutputStream("dados/temp2.db"));
    DataOutputStream out3 = new DataOutputStream(new FileOutputStream("dados/temp3.db"));
    DataOutputStream out = null;

    try {
      contador = 0;
      seletor = 0;
      while (true) {

        // Lê o registro no arquivo de dados
        lapide = arquivo.readByte();
        tamanho = arquivo.readShort();
        dados = new byte[tamanho];
        arquivo.read(dados);
        r1.fromByteArray(dados);

        // Adiciona o registro ao vetor
        if (lapide == ' ') {
          registrosOrdenados.add((T) r1.clone());
          contador++;
        }
        if (contador == tamanhoBlocoMemoria) {

          switch (seletor) {
            case 0:
              out = out1;
              break;
            case 1:
              out = out2;
              break;
            default:
              out = out3;
          }
          seletor = (seletor + 1) % 3;

          Collections.sort(registrosOrdenados);
          for (T r : registrosOrdenados) {
            dados = r.toByteArray();
            out.writeShort(dados.length);
            out.write(dados);
          }
          registrosOrdenados.clear();

          contador = 0;
        }

      }

    } catch (EOFException eof) {
      // Descarrega os últimos registros lidos
      if (contador > 0) {
        switch (seletor) {
          case 0:
            out = out1;
            break;
          case 1:
            out = out2;
            break;
          default:
            out = out3;
        }

        Collections.sort(registrosOrdenados);
        for (T r : registrosOrdenados) {
          dados = r.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
        }
      }
    }
    out1.close();
    out2.close();
    out3.close();

    // ---------------------------------------------------------------------
    // Segunda etapa (intercalação)
    // ---------------------------------------------------------------------
    DataInputStream in1, in2, in3;
    boolean sentido = true; // true: conj1 -> conj2 | false: conj2 -> conj1
    boolean maisIntercalacoes = true;
    boolean compara1, compara2, compara3;
    boolean terminou1, terminou2, terminou3;

    while (maisIntercalacoes) {

      maisIntercalacoes = false;
      compara1 = false;
      compara2 = false;
      compara3 = false;
      terminou1 = false;
      terminou2 = false;
      terminou3 = false;

      // Seleciona as fontes e os destinos
      if (sentido) {
        in1 = new DataInputStream(new FileInputStream("dados/temp1.db"));
        in2 = new DataInputStream(new FileInputStream("dados/temp2.db"));
        in3 = new DataInputStream(new FileInputStream("dados/temp3.db"));
        out1 = new DataOutputStream(new FileOutputStream("dados/temp4.db"));
        out2 = new DataOutputStream(new FileOutputStream("dados/temp5.db"));
        out3 = new DataOutputStream(new FileOutputStream("dados/temp6.db"));
      } else {
        in1 = new DataInputStream(new FileInputStream("dados/temp4.db"));
        in2 = new DataInputStream(new FileInputStream("dados/temp5.db"));
        in3 = new DataInputStream(new FileInputStream("dados/temp6.db"));
        out1 = new DataOutputStream(new FileOutputStream("dados/temp1.db"));
        out2 = new DataOutputStream(new FileOutputStream("dados/temp2.db"));
        out3 = new DataOutputStream(new FileOutputStream("dados/temp3.db"));
      }
      sentido = !sentido;
      seletor = 0;

      // novos registros anteriores vazios
      r1 = construtor.newInstance();
      r2 = construtor.newInstance();
      r3 = construtor.newInstance();

      // Inicia a intercalação dos segmentos
      boolean mudou1 = true, mudou2 = true, mudou3 = true;
      while (!terminou1 || !terminou2 || !terminou3) {

        if (!compara1 && !compara2 && !compara3) {
          // Seleciona o próximo arquivo de saída
          switch (seletor) {
            case 0:
              out = out1;
              break;
            case 1:
              out = out2;
              break;
            default:
              out = out3;
          }
          seletor = (seletor + 1) % 3;

          if (!terminou1)
            compara1 = true;
          if (!terminou2)
            compara2 = true;
          if (!terminou3)
            compara3 = true;
        }

        // le o próximo registro da última fonte usada
        if (mudou1) {
          rAnt1 = (T) r1.clone();
          try {
            tamanho = in1.readShort();
            dados = new byte[tamanho];
            in1.read(dados);
            r1.fromByteArray(dados);
            if (r1.compareTo(rAnt1) < 0)
              compara1 = false;
          } catch (EOFException e) {
            compara1 = false;
            terminou1 = true;
          }
          mudou1 = false;
        }
        if (mudou2) {
          rAnt2 = (T) r2.clone();
          try {
            tamanho = in2.readShort();
            dados = new byte[tamanho];
            in2.read(dados);
            r2.fromByteArray(dados);
            if (r2.compareTo(rAnt2) < 0)
              compara2 = false;
          } catch (EOFException e) {
            compara2 = false;
            terminou2 = true;
          }
          mudou2 = false;
        }
        if (mudou3) {
          rAnt3 = (T) r3.clone();
          try {
            tamanho = in3.readShort();
            dados = new byte[tamanho];
            in3.read(dados);
            r3.fromByteArray(dados);
            if (r3.compareTo(rAnt3) < 0)
              compara3 = false;
          } catch (EOFException e) {
            compara3 = false;
            terminou3 = true;
          }
          mudou3 = false;
        }

        // Escreve o menor registro
        if (compara1 && (!compara2 || r1.compareTo(r2) <= 0) && (!compara3 || r1.compareTo(r3) <= 0)) {
          dados = r1.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
          mudou1 = true;
        } else if (compara2 && (!compara1 || r2.compareTo(r1) <= 0) && (!compara3 || r2.compareTo(r3) <= 0)) {
          dados = r2.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
          mudou2 = true;
        } else if (compara3 && (!compara1 || r3.compareTo(r1) <= 0) && (!compara2 || r3.compareTo(r2) <= 0)) {
          dados = r3.toByteArray();
          out.writeShort(dados.length);
          out.write(dados);
          mudou3 = true;
        }

        // Testa se há mais intercalações a fazer
        if (seletor > 1)
          maisIntercalacoes = true;
      }

      in1.close();
      in2.close();
      in3.close();
      out1.close();
      out2.close();
      out3.close();
    }

    // return;

    // copia os registros de volta para o arquivo original
    if (sentido)
      in1 = new DataInputStream(new FileInputStream("dados/temp1.db"));
    else
      in1 = new DataInputStream(new FileInputStream("dados/temp4.db"));

    arquivo.close();
    new File("dados/" + nomeEntidade + ".db").delete();
    DataOutputStream ordenado = new DataOutputStream(new FileOutputStream(nomeEntidade));
    ordenado.write(ba_cabecalho);

    indiceDireto.close();
    new File("dados/" + nomeEntidade + ".hash_d.db").delete();
    new File("dados/" + nomeEntidade + ".hash_c.db").delete();
    indiceDireto = new HashExtensivel<>(ParIDEndereco.class.getConstructor(),
        4, "dados/" + this.nomeEntidade + ".hash_d.db",
        "dados/" + this.nomeEntidade + ".hash_c.db");

    long endereco;
    try {
      while (true) {
        tamanho = in1.readShort();
        dados = new byte[tamanho];
        in1.read(dados);
        r1.fromByteArray(dados);

        endereco = ordenado.size();
        ordenado.writeByte(' '); // lápide
        ordenado.writeShort(tamanho);
        ordenado.write(dados);
        indiceDireto.create(new ParIDEndereco(r1.getID(), endereco));
      }
    } catch (EOFException e) {
      // saída normal
    }
    ordenado.close();
    in1.close();
    (new File("dados/temp1.db")).delete();
    (new File("dados/temp2.db")).delete();
    (new File("dados/temp3.db")).delete();
    (new File("dados/temp4.db")).delete();
    (new File("dados/temp5.db")).delete();
    (new File("dados/temp6.db")).delete();
    arquivo = new RandomAccessFile("dados/" + this.nomeEntidade + ".db", "rw");
  }

}
