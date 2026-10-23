package br.universidade.approomproduto.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.universidade.approomproduto.model.Product

@Dao
interface ProductDao {

    // suspend: o Room nao permite consultas na main thread
    @Query("SELECT * FROM product")
    suspend fun getAll(): List<Product>

    // A5: busca por nome (use "%texto%" para nomes parecidos)
    @Query("SELECT * FROM product WHERE name LIKE :nome")
    suspend fun buscarPorNome(nome: String): List<Product>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: Int): Product?

    @Insert
    suspend fun insertAll(vararg products: Product)

    // A7: atualizar produto
    @Update
    suspend fun update(product: Product): Int

    // A6 (desafio extra): excluir um produto especifico
    @Delete
    suspend fun delete(product: Product): Int

    // A6: excluir todos
    @Query("DELETE FROM product")
    suspend fun deleteAll()
}
