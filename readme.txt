TP1 
Arthur Leite Setragni
Gustavo Garcia Macedo

• O que você considerou como perda aceitável para o reuso de espaços vazios, isto é, quais são os critérios para a gestão dos espaços vazios?

    : Consideramos uma perda aceitável de 25% dos bytes. Isso quer dizer que quando um novo registro cabe no espaço vazio e representa 75% ou mais desse espaço ele é salvo nesse local

• O código do CRUD com arquivos de tipos genéricos está funcionando corretamente?
    
    :Sim

• O CRUD tem um índice direto implementado com a tabela hash extensível?

    :Sim

• A operação de inclusão busca o espaço vazio mais adequado para o novo registro antes de acrescentá-lo ao fim do arquivo?

    :Nossas funções buscam o primeiro espaço vazio que se adeque segundo nossos criterios, e insere o registro nesse espaço, apenas caso não exista um espaço compativel o registro é inserido no fim do arquivo.

• A operação de alteração busca o espaço vazio mais adequado para o registro quando ele cresce de tamanho antes de acrescentá-lo ao fim do arquivo?

    :Nossas funções buscam o primeiro espaço vazio que se adeque segundo nossos criterios, e insere o registro nesse espaço, apenas caso não exista um espaço compativel o registro é inserido no fim do arquivo.

• As operações de alteração (quando for o caso) e de exclusão estão gerenciando os espaços vazios para que possam ser reaproveitados?

    :Sim, os endereços dos espaços vazios e seus respectivos tamanhos estão sendo guardados para que os espaços vazios sejam reaproveitados.

• O trabalho está funcionando corretamente?

    :Sim, todas as funções do CRUD estão funcionando corretamente.

• O trabalho está completo?

    :Sim, o trabalho está completo.

• O trabalho é original e não a cópia de um trabalho de um colega?

    : O trabalho é de autoria dos alunos Arthur Leite e Gustavo Garcia
