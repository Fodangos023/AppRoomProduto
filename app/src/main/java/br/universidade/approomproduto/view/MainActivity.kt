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
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
// Inicializa o banco de dados Room de forma recomendada (sem usar allowMainThreadQueries)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration() // Usado para recriar o DB se o esquema mudar
            .build()
        productDao = db.productDao()
// Exemplo de inserção de produto
        binding.btnInserirProduto.setOnClickListener {
            val name = binding.editProductName.text.toString()
            val priceText = binding.editProductPrice.text.toString()
            val description = binding.editProductDescription.text.toString()
// Verifica se os campos estão preenchidos
            if (name.isNotEmpty() && priceText.isNotEmpty()) {
                val price = priceText.toDoubleOrNull() ?: 0.0
                val product = Product(name = name, price = price, description = description)
// Insere o produto no banco de dados de forma assíncrona (usando corrotina)
                lifecycleScope.launch {
                    productDao.insertAll(product)
                    Log.i("TAG", "Produto inserido: $name - R$$price")
                    Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Preencha os campos obrigatórios!", Toast.LENGTH_SHORT).show()
            }
        }
// Exemplo de recuperação de produtos
        binding.btnListarProdutos.setOnClickListener {
// Recupera os produtos de forma assíncrona
            lifecycleScope.launch {
                val products = productDao.getAll()
                if (products.isNotEmpty()) {
                    products.forEach {
                        Log.i("TAG", "Produto = ${it.name}, Preço = R$${it.price}, Descrição = ${it.description}")
                    }
                } else {
                    Log.i("TAG", "Nenhum produto encontrado.")
                }
            }
        }
    }
}