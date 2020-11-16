package net.odbogm.proxy;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ILazyCalls {
    public boolean isDirty();
    public void clearState();
    public void rollback();
    public void forceLoad();
}
