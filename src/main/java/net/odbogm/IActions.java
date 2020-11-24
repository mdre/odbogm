package net.odbogm;

import java.util.List;
import java.util.Map;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.exceptions.VertexJavaClassNotFound;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface IActions {

    public interface IStore {

        /**
         * Guarda un objeto en la base de datos descomponiéndolo en vértices y links y retorna el @RID asociado.
         *
         * @param <T> clase del objeto.
         * @param o objeto a almacenar
         * @return retorna el nuevo objeto administrado por la base.
         * @throws IncorrectRIDField referencia incorrecta.
         */
        <T> T store(T o) throws IncorrectRIDField;
    }

    public interface IDelete {

        <T> void delete(T object);

        <T> void deleteAll(Class<T> type);

        void purgeDatabase();

        void clear();
    }

    public interface IGet {
        
        // load a single object of Class type, with id id
        Object get(String rid) throws UnknownRID;

        <T> T get(Class<T> type, String rid) throws UnknownRID, VertexJavaClassNotFound;
        
        <T> T fetch(Class<T> type, String rid);
        
    }

    public interface IQuery {

        /**
         * Realiza un query direto a la base de datos y devuelve el resultado directamente sin procesarlo.
         *
         * @param <T> clase a devolver
         * @param sql sentencia a ejecutar
         * @return resutado de la ejecución de la sentencia SQL
         */
        public <T> T query(String sql);
        
        /**
         * Realiza un query direto a la base de datos y devuelve el resultado directamente sin procesarlo.
         *
         * @param <T> clase a devolver
         * @param sql sentencia a ejecutar
         * @param param parámetros a usar en el prepared query.
         * @return resutado de la ejecución de la sentencia SQL
         */
        public <T> T query(String sql, Object... param);

        /**
         * Ejecuta un comando que devuelve un número. El valor devuelto será el primero que se encuentre en la lista de resultado.
         *
         * @param sql comando a ejecutar
         * @param retVal nombre de la propiedad a devolver
         * @return retorna el valor de la propiedad indacada obtenida de la ejecución de la consulta
         *
         * ejemplo: int size = sm.query("select count(*) as size from TestData","size");
         */
        public long query(String sql, String retVal);

        /**
         * Return all record of the reference class. Devuelve todos los registros a partir de una clase base.
         *
         * @param <T> Reference class
         * @param clazz reference class
         * @return return a list of object of the refecence class.
         */
        public <T> List<T> query(Class<T> clazz);

        /**
         * Devuelve todos los registros a partir de una clase base en una lista, filtrando los datos por lo que se agregue en el body.
         *
         * @param <T> clase base que se utilizará para el armado de la lista
         * @param clase clase base.
         * @param body cuerpo a agregar a la sentencia select. Ej: "where ...."
         * @return Lista con todos los objetos recuperados.
         */
        public <T> List<T> query(Class<T> clase, String body);

        /**
         * Ejecuta un prepared query y devuelve una lista de la clase indicada.
         *
         * @param <T> clase de referencia para crear la lista de resultados
         * @param clase clase de referencia
         * @param sql comando a ejecutar
         * @param param parámetros extras para el query parametrizado.
         * @return una lista de la clase solicitada con los objetos lazy inicializados.
         */
        public <T> List<T> query(Class<T> clase, String sql, Object... param);
        
        /**
         * Ejecuta un prepared query y devuelve una lista de la clase indicada.
         * Esta consulta acepta parámetros por nombre. 
         * Ej:
         * <pre> {@code 
         *  Map<String, Object> params = new HashMap<String, Object>();
         *  params.put("theName", "John");
         *  params.put("theSurname", "Smith");
         *
         *  graph.command(
         *       new OCommandSQL("UPDATE Customer SET local = true WHERE name = :theName and surname = :theSurname")
         *      ).execute(params)
         *  );
         *  }
         * </pre>
         * @param <T> clase de referencia para crear la lista de resultados
         * @param clase clase de referencia
         * @param sql comando a ejecutar
         * @param params parámetros extras para el query parametrizado.
         * @return una lista de la clase solicitada con los objetos lazy inicializados.
         */
        public <T> List<T> query(Class<T> clase, String sql, Map params);
    }

}
