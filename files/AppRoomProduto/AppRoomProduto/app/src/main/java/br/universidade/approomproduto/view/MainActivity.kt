package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cria (ou abre) o banco Room sem usar allowMainThreadQueries
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration() // recria o DB se o esquema mudar (so p/ desenvolvimento)
            .build()
        productDao = db.productDao()

        configurarInserir()
        configurarListar()
        configurarBuscar()
        configurarAtualizar()
        configurarExcluir()
    }

    // ---------- Inserir (A1 + A2 + A8) ----------
    private fun configurarInserir() {
        binding.btnInserirProduto.setOnClickListener {
            val name = binding.editProductName.text.toString().trim()
            val description = binding.editProductDescription.text.toString().trim()
            val quantity = binding.editProductQuantity.text.toString().toIntOrNull() ?: 0

            if (name.isEmpty()) {
                binding.editProductName.error = "Informe o nome do produto"
                return@setOnClickListener
            }

            // A2: preco vazio, zero ou negativo nao e permitido
            val price = lerPreco(binding.editProductPrice.text.toString())
            if (price == null) {
                binding.editProductPrice.error = "Informe um preço válido"
                return@setOnClickListener
            }

            val product = Product(
                name = name,
                price = price,
                description = description.ifEmpty { null },
                quantity = quantity
            )

            // Insere de forma assincrona (corrotina)
            lifecycleScope.launch {
                productDao.insertAll(product)
                Log.i("TAG", "Produto inserido: $name - ${formatarPreco(price)}")
                Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()
                limparCampos() // A1
            }
        }
    }

    // A1: limpa os campos e devolve o cursor ao campo Nome
    private fun limparCampos() {
        binding.editProductName.text.clear()
        binding.editProductPrice.text.clear()
        binding.editProductDescription.text.clear()
        binding.editProductQuantity.text.clear()
        binding.editProductName.requestFocus()
    }

    // ---------- Listar (A3 + A4) ----------
    private fun configurarListar() {
        binding.btnListarProdutos.setOnClickListener {
            lifecycleScope.launch {
                mostrarLista("Lista de produtos:", productDao.getAll())
            }
        }
    }

    // ---------- Buscar por nome (A5) ----------
    private fun configurarBuscar() {
        binding.btnBuscar.setOnClickListener {
            val texto = binding.editBuscaNome.text.toString().trim()
            lifecycleScope.launch {
                val resultado = productDao.buscarPorNome("%$texto%")
                mostrarLista("Resultado da busca por \"$texto\":", resultado)
            }
        }
    }

    // ---------- Atualizar preco (A7) ----------
    private fun configurarAtualizar() {
        binding.btnAtualizarPreco.setOnClickListener {
            val id = binding.editIdProduto.text.toString().toIntOrNull()
            if (id == null) {
                binding.editIdProduto.error = "Informe o id do produto"
                return@setOnClickListener
            }
            val novoPreco = lerPreco(binding.editNovoPreco.text.toString())
            if (novoPreco == null) {
                binding.editNovoPreco.error = "Informe um preço válido"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val atual = productDao.getById(id)
                if (atual == null) {
                    Toast.makeText(this@MainActivity, "Produto não encontrado!", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                productDao.update(atual.copy(price = novoPreco))
                Toast.makeText(this@MainActivity, "Preço atualizado!", Toast.LENGTH_SHORT).show()
                binding.editNovoPreco.text.clear()
                mostrarLista("Lista de produtos:", productDao.getAll())
            }
        }
    }

    // ---------- Excluir (A6) ----------
    private fun configurarExcluir() {
        binding.btnExcluirTodos.setOnClickListener {
            lifecycleScope.launch {
                productDao.deleteAll()
                binding.textResultado.text = "Todos os produtos foram excluídos."
                Toast.makeText(this@MainActivity, "Produtos excluídos!", Toast.LENGTH_SHORT).show()
            }
        }

        // Desafio extra: excluir um produto especifico pelo id (@Delete)
        binding.btnExcluirPorId.setOnClickListener {
            val id = binding.editIdProduto.text.toString().toIntOrNull()
            if (id == null) {
                binding.editIdProduto.error = "Informe o id do produto"
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val produto = productDao.getById(id)
                if (produto == null) {
                    Toast.makeText(this@MainActivity, "Produto não encontrado!", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                productDao.delete(produto)
                Toast.makeText(this@MainActivity, "Produto excluído!", Toast.LENGTH_SHORT).show()
                mostrarLista("Lista de produtos:", productDao.getAll())
            }
        }
    }

    // ---------- Auxiliares ----------

    // Retorna o preco se for valido (> 0); caso contrario, null
    private fun lerPreco(texto: String): Double? {
        val valor = texto.trim().replace(',', '.').toDoubleOrNull()
        return if (valor != null && valor > 0.0) valor else null
    }

    private fun formatarPreco(valor: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

    // A3 + A4: mostra a lista no TextView (e no Logcat), com quantidade e soma dos precos
    private fun mostrarLista(titulo: String, products: List<Product>) {
        if (products.isEmpty()) {
            binding.textResultado.text = "$titulo\nNenhum produto encontrado."
            Log.i("TAG", "Nenhum produto encontrado.")
            return
        }

        val total = products.sumOf { it.price }
        val texto = StringBuilder()
        texto.appendLine(titulo)
        texto.appendLine("Produtos cadastrados: ${products.size}")
        texto.appendLine("Soma dos preços: ${formatarPreco(total)}")
        texto.appendLine()

        products.forEach {
            texto.appendLine("#${it.id} - ${it.name} - ${formatarPreco(it.price)} - Qtd: ${it.quantity}")
            texto.appendLine("     ${it.description ?: "(sem descrição)"}")
            Log.i(
                "TAG",
                "Produto = ${it.name}, Preço = ${formatarPreco(it.price)}, " +
                    "Qtd = ${it.quantity}, Descrição = ${it.description}"
            )
        }
        binding.textResultado.text = texto.toString()
    }
}
