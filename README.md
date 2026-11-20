# LastRest

Mod de lápides para **Minecraft Forge 1.20.1 / Forge 47.4.10**, com Java 17.

## Comportamento implementado no código

- Cada morte com itens cria sua própria lápide de pedra.
- Guarda os itens da hotbar, inventário, armadura e mão secundária, mantendo NBT, encantamentos, nomes, dano e quantidades.
- Ao interagir, primeiro combina com pilhas compatíveis; depois ocupa espaços vazios da mochila.
- Se o inventário não comportar tudo, os itens restantes continuam na lápide.
- A lápide some quando fica vazia.
- Cada lápide tem 36.000 ticks de vida (30 minutos a 20 TPS), contados pelo servidor e pausados quando o mundo está fechado.
- Os dados ficam no salvamento do mundo; o temporizador continua mesmo com o chunk descarregado.
- HUD 2D mostra direção, distância e nome do dono de todas as lápides da dimensão atual.
- Page Up e Page Down percorrem as páginas do HUD quando há muitas lápides.
- A lápide não força carregamento de chunks.

## Compilar

Requer JDK 17 e Gradle 8.8:

```sh
gradle build
```

O arquivo final será gerado em `build/libs/LastRest-1.0.0.jar`.

## Atenção: estado de testes

O pipeline de CI compila e empacota o mod. Isso não substitui os testes em um cliente Forge e em um servidor dedicado/LAN. A mecânica de morte, recuperação, salvamento, HUD e expiração ainda precisa passar por testes reais em jogo antes de recomendar o uso em um mundo principal.
