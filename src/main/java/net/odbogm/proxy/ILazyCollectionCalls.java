package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.ODirection;
import java.util.Map;
import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ILazyCollectionCalls extends ILazyCalls {
    
    /**
     * Inicializa la colección y la establece como lazy.
     * 
     * @param sm        Transacción sobre la cual realizar los pedidos 
     * @param parent    Objeto relacionado al que notificar los cambios
     * @param field     campo a procesar
     * @param c         clase asociada al campo
     * @param d         Si es OUT, se notifican los cambios. Si es IN, se toma como una colección 
     *                  indirecta y SE IGNORAN LOS CAMBIOS.
     */
    public void init(Transaction sm, IObjectProxy parent, String field, Class<?> c, ODirection d);
    public Map<Object, ObjectCollectionState> collectionState();
    
}
