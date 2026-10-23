# AppRoomProduto: Cadastro de Produtos com Room SQLite

**Aluno(a):** Seu Nome Completo
**RA:** 000000
**Disciplina:** Programação para Dispositivos Móveis
**Professor(a):** Nome do Professor

## Sobre o projeto

Aplicativo Android em Kotlin que cadastra, lista, busca, atualiza e exclui produtos
usando persistência local com Room (SQLite). Os dados continuam salvos depois de fechar o app.

## Tecnologias

- Kotlin
- Room 2.6.1 (SQLite)
- ViewBinding
- Corrotinas (lifecycleScope)

## Estrutura

```
app/src/main/
├── java/br/universidade/approomproduto/
│   ├── model/Product.kt        ← tabela (Entidade)
│   ├── dao/ProductDao.kt       ← comandos SQL (DAO)
│   ├── db/AppDatabase.kt       ← o banco de dados
│   └── view/MainActivity.kt    ← a tela
├── res/layout/activity_main.xml
└── AndroidManifest.xml
```

## Prints do app

![Tela inicial](docs/prints/tela-inicial.png)
![Logcat com a listagem](docs/prints/logcat.png)

---

## Parte A: Complemento prático

### A1. Limpar campos após inserir

**O que foi feito:** depois que `insertAll` termina com sucesso, a função `limparCampos()` apaga o texto dos quatro campos e chama `requestFocus()` no campo Nome, deixando o app pronto para o próximo cadastro.

```kotlin
private fun limparCampos() {
    binding.editProductName.text.clear()
    binding.editProductPrice.text.clear()
    binding.editProductDescription.text.clear()
    binding.editProductQuantity.text.clear()
    binding.editProductName.requestFocus()
}
```

Print: ![A1](docs/prints/a1.png)

### A2. Validação de preço

**O que foi feito:** a função `lerPreco()` converte o texto para `Double` (aceitando vírgula ou ponto) e devolve `null` se o valor estiver vazio, for zero ou negativo. Quando é `null`, o erro aparece no próprio campo e nada é salvo.

```kotlin
private fun lerPreco(texto: String): Double? {
    val valor = texto.trim().replace(',', '.').toDoubleOrNull()
    return if (valor != null && valor > 0.0) valor else null
}

val price = lerPreco(binding.editProductPrice.text.toString())
if (price == null) {
    binding.editProductPrice.error = "Informe um preço válido"
    return@setOnClickListener
}
```

Print: ![A2](docs/prints/a2.png)

### A3. Lista na tela

**O que foi feito:** adicionei um `TextView` (`textResultado`) abaixo dos botões. Ao clicar em Listar Produtos, a função `mostrarLista()` monta um texto com todos os produtos e o coloca nele. O Logcat continua recebendo as mesmas linhas.

```kotlin
products.forEach {
    texto.appendLine("#${it.id} - ${it.name} - ${formatarPreco(it.price)} - Qtd: ${it.quantity}")
    texto.appendLine("     ${it.description ?: "(sem descrição)"}")
}
binding.textResultado.text = texto.toString()
```

Print: ![A3](docs/prints/a3.png)

### A4. Total do estoque

**O que foi feito:** no começo do texto da lista, `mostrarLista()` mostra quantos produtos existem (`products.size`) e a soma dos preços (`products.sumOf { it.price }`).

```kotlin
val total = products.sumOf { it.price }
texto.appendLine("Produtos cadastrados: ${products.size}")
texto.appendLine("Soma dos preços: ${formatarPreco(total)}")
```

Print: ![A4](docs/prints/a4.png)

### A5. Buscar por nome

**O que foi feito:** criei a consulta `buscarPorNome` no DAO, um campo de busca e o botão Buscar. O texto digitado é cercado por `%` para encontrar nomes parecidos (por exemplo, "tec" encontra "Teclado").

```kotlin
// ProductDao.kt
@Query("SELECT * FROM product WHERE name LIKE :nome")
suspend fun buscarPorNome(nome: String): List<Product>

// MainActivity.kt
val resultado = productDao.buscarPorNome("%$texto%")
mostrarLista("Resultado da busca por \"$texto\":", resultado)
```

Print: ![A5](docs/prints/a5.png)

### A6. Excluir produtos

**O que foi feito:** criei `deleteAll()` (botão Excluir Todos). Como desafio extra, criei também o botão Excluir por Id: o app busca o produto com `getById` e o apaga com `@Delete`.

```kotlin
// ProductDao.kt
@Query("DELETE FROM product")
suspend fun deleteAll()

@Delete
suspend fun delete(product: Product): Int

// MainActivity.kt
val produto = productDao.getById(id)
if (produto != null) productDao.delete(produto)
```

Print: ![A6](docs/prints/a6.png)

### A7. Atualizar produto

**O que foi feito:** criei `@Update` no DAO. O usuário informa o id e o novo preço; o app busca o produto, cria uma cópia com o preço novo (`copy`) e grava. O novo preço passa pela mesma validação do A2.

```kotlin
// ProductDao.kt
@Update
suspend fun update(product: Product): Int

// MainActivity.kt
val atual = productDao.getById(id)
if (atual != null) {
    productDao.update(atual.copy(price = novoPreco))
}
```

Print: ![A7](docs/prints/a7.png)

### A8. Novo campo na tabela (quantity)

**O que foi feito:** adicionei `quantity: Int` na entidade `Product`, um campo "Quantidade em estoque" no layout, e aumentei `version` de 1 para 2 no `@Database`. A quantidade aparece na lista.

```kotlin
// Product.kt
@ColumnInfo(name = "quantity") val quantity: Int = 0

// AppDatabase.kt
@Database(entities = [Product::class], version = 2)
```

**O que aconteceu com os produtos já cadastrados e por quê:** eles foram apagados. Quando o app abriu com `version = 2`, o Room viu que o banco gravado no celular ainda estava na versão 1 e que a estrutura da tabela mudou. Como não existe nenhuma `Migration` de 1 para 2 e o banco foi criado com `fallbackToDestructiveMigration()`, o Room apagou as tabelas antigas e criou as novas, já com a coluna `quantity`. Por isso, ao clicar em Listar Produtos logo depois da mudança, apareceu "Nenhum produto encontrado". Sem o `fallbackToDestructiveMigration()`, o app fecharia com um erro dizendo que uma migração de 1 para 2 era necessária e não foi encontrada.

Print: ![A8](docs/prints/a8.png)

---

## Parte B: Questões dissertativas

### B1. O que é persistência de dados? O que aconteceria se os produtos ficassem só em uma MutableList na MainActivity?

**Resposta:** Persistência de dados é a capacidade de guardar informações de modo que elas continuem existindo depois que o programa é fechado, o celular é reiniciado ou o sistema encerra o processo. Em vez de ficar apenas na memória RAM, o dado é gravado em um armazenamento permanente, como o arquivo `database-name` que o Room cria no armazenamento interno do app. Se os produtos estivessem só em uma `MutableList` dentro da `MainActivity`, eles viveriam apenas na memória enquanto a Activity existisse. Bastaria fechar o app, o Android matar o processo para liberar memória ou até girar a tela (a Activity é recriada) para a lista voltar vazia e todos os cadastros serem perdidos. No meu projeto, o `productDao.insertAll(product)` grava o produto no arquivo do banco, e por isso, depois de fechar e abrir o app, o botão Listar Produtos ainda mostra tudo que foi cadastrado.

### B2. Qual é a diferença entre usar o SQLite diretamente e usar o Room? Cite pelo menos duas vantagens do Room.

**Resposta:** Com o SQLite puro é preciso criar uma classe que herda de `SQLiteOpenHelper`, escrever à mão o `CREATE TABLE` em uma string, tratar `onUpgrade`, montar `ContentValues` para inserir e percorrer um `Cursor` coluna por coluna para transformar cada linha em um objeto. Tudo isso é muito código repetitivo, e um erro de digitação em uma consulta SQL só aparece quando o app está rodando. O Room é uma camada sobre o SQLite que faz esse trabalho por mim: eu descrevo a tabela com uma classe anotada com `@Entity` e os comandos com `@Dao`, e ele gera o código na compilação. Entre as vantagens estão: (1) as consultas `@Query` são verificadas em tempo de compilação, então um nome de tabela ou coluna errado impede o build em vez de derrubar o app; (2) a conversão entre linha do banco e objeto Kotlin é automática, o que reduz muito o código; (3) ele se integra com corrotinas (`suspend`), Flow e LiveData e avisa quando algum acesso é feito na thread principal; (4) tem suporte a migrações de versão.

### B3. Explique o papel de Entity, DAO e Database e como se relacionam no projeto.

**Resposta:** A **Entity** é a classe que representa a tabela. No projeto, `Product` é anotada com `@Entity`, cada atributo (`id`, `name`, `price`, `description`, `quantity`) vira uma coluna e cada objeto criado vira uma linha. O **DAO** (Data Access Object) é a interface onde ficam as operações que posso fazer com os dados: no projeto, `ProductDao` tem `getAll`, `buscarPorNome`, `insertAll`, `update`, `delete` e `deleteAll`, e o Room escreve a implementação delas. O **Database** é a classe abstrata `AppDatabase`, anotada com `@Database`, que junta as peças: ela lista as entidades (`entities = [Product::class]`), define a versão e expõe o DAO pela função `productDao()`. A relação é: o `AppDatabase` conhece a entidade e entrega o DAO; a `MainActivity` constrói o banco com `Room.databaseBuilder`, pega o DAO com `db.productDao()` e usa esse DAO para gravar e ler objetos `Product`.

### B4. Por que o Android não permite acessar o banco na thread principal? O que é ANR? Como suspend e lifecycleScope.launch resolvem?

**Resposta:** A thread principal (UI thread) é a que desenha a tela e responde aos toques do usuário. Ler ou gravar em disco pode ser lento, e se isso acontecer nela, a interface congela durante a operação. Se o app fica sem responder por tempo demais (por volta de 5 segundos para eventos de toque), o Android mostra o erro **ANR (Application Not Responding)**, oferecendo ao usuário fechar o app. Por isso o Room lança "Cannot access database on the main thread" quando o acesso é feito ali. A solução é fazer o acesso ao banco fora da thread principal. As funções do DAO marcadas com `suspend` são executadas pelo Room em uma thread de segundo plano e suspendem a corrotina enquanto esperam, sem bloquear a tela. O `lifecycleScope.launch { }` abre essa corrotina presa ao ciclo de vida da Activity: ela começa na thread principal, pode chamar as funções `suspend` com segurança, volta com o resultado para atualizar a tela (como no `Toast` e no `TextView`) e é cancelada automaticamente se a Activity for destruída, evitando vazamento de memória e erros.

### B5. O que faz o fallbackToDestructiveMigration()? Por que é perigoso na Play Store? O que é uma Migration?

**Resposta:** Quando o número de `version` do banco aumenta e o Room não encontra uma migração para ir da versão antiga à nova, o `fallbackToDestructiveMigration()` manda apagar todas as tabelas e recriá-las vazias com a estrutura nova. Em desenvolvimento isso é prático, mas em um app publicado é perigoso: a cada atualização que muda a estrutura, todos os usuários perderiam os dados que já cadastraram (produtos, histórico, etc.), o que é uma perda grave e gera avaliações negativas. Uma **Migration** é um objeto que ensina o Room a converter o banco de uma versão para outra preservando os dados, usando comandos SQL. No meu projeto, por exemplo, uma migração de 1 para 2 seria `Migration(1, 2) { db -> db.execSQL("ALTER TABLE product ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0") }`, registrada com `.addMigrations(...)` no `databaseBuilder`. Assim os produtos antigos seriam mantidos, com quantidade 0.

### B6. Qual é a vantagem do ViewBinding em relação ao findViewById? Que erros ele ajuda a evitar?

**Resposta:** Com `findViewById` eu preciso informar o id e, muitas vezes, converter o tipo da view, e nada disso é verificado antes de rodar. O ViewBinding gera, para cada layout, uma classe (no projeto, `ActivityMainBinding`) com um atributo para cada componente que tem id, já com o tipo correto. Assim escrevo `binding.editProductName.text` direto, sem procurar a view e sem conversão. Ele ajuda a evitar principalmente: `NullPointerException` causado por id que não existe no layout carregado, `ClassCastException` por tratar uma view como o tipo errado, e erros de digitação no id, que viram erro de compilação em vez de falha com o app aberto. Também deixa o código mais curto e fácil de ler, e como o acesso é direto à classe gerada, o Android Studio consegue autocompletar os componentes.

### B7. Por que em apps maiores o banco deve ser Singleton? O que poderia acontecer se várias telas criassem cada uma o seu banco?

**Resposta:** Uma instância do `RoomDatabase` é cara: ela abre conexões com o arquivo, cria threads e mantém controle interno das mudanças. Por isso a recomendação é ter apenas uma instância para o app inteiro (Singleton), normalmente com um `companion object` que guarda a instância e a cria só na primeira vez, de forma sincronizada. Se cada tela criasse o seu próprio banco, o app gastaria mais memória e abriria várias conexões para o mesmo arquivo. Poderiam surgir problemas de bloqueio e concorrência de escrita (erros como "database is locked"), e as telas não saberiam das mudanças feitas pelas outras, porque os mecanismos de observação (LiveData/Flow) ficam presos a cada instância, deixando uma tela mostrando dados desatualizados. No meu projeto o banco é criado na `MainActivity`, o que funciona porque só existe uma tela, mas se o app crescesse eu moveria essa criação para um Singleton ou para injeção de dependência (como o Hilt).

### B8. Reflexão pessoal

**Dificuldades e como resolvi:**

> ✏️ **ESCREVA AQUI com suas palavras** as dificuldades que você teve de verdade (por exemplo: erro de Gradle/kapt ao sincronizar, `Unresolved reference: ActivityMainBinding`, filtro `tag:TAG` do Logcat, entender `suspend`) e como você resolveu cada uma.

**App real onde o Room seria útil:** um aplicativo de **controle de estoque para uma pequena loja**. Ele usaria quase a mesma estrutura deste projeto: a entidade `Product` (com nome, preço, quantidade e descrição), um DAO com cadastro, busca por nome, atualização de quantidade e exclusão, e um `RecyclerView` no lugar do `TextView` para mostrar a lista. O dono da loja poderia registrar entradas e saídas de mercadoria e consultar o estoque mesmo sem internet, algo comum em lojas pequenas ou em depósitos com sinal fraco. Eu também criaria uma tabela de movimentações (venda, reposição) ligada ao produto e uma `Migration` quando a estrutura mudasse, para nunca apagar os dados do lojista.

---

## Como executar

1. Clone o repositório
2. Abra no Android Studio
3. Execute em um emulador (API 24+)
