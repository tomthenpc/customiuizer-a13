package tv.withaibuild.customiuizer.mods;

import java.lang.reflect.Executable;
import java.util.Collections;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

public class FakeChain implements XposedInterface.Chain {

    private final Executable executable;
    private final Object thisObject;
    private final List<Object> args;
    private final Object result;
    private final Throwable failure;

    public boolean proceeded = false;
    public Object[] proceedArgs = null;

    public FakeChain(Executable executable, Object thisObject, List<Object> args, Object result, Throwable failure) {
        this.executable = executable;
        this.thisObject = thisObject;
        this.args = args != null ? args : Collections.emptyList();
        this.result = result;
        this.failure = failure;
    }

    @Override
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public Object getThisObject() {
        return thisObject;
    }

    @Override
    public List<Object> getArgs() {
        return args;
    }

    @Override
    public Object getArg(int index) {
        return args.get(index);
    }

    @Override
    public Object proceed() throws Throwable {
        proceeded = true;
        if (failure != null) throw failure;
        return result;
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
        proceedArgs = args;
        return proceed();
    }

    @Override
    public Object proceedWith(Object thisObject) throws Throwable {
        return proceed();
    }

    @Override
    public Object proceedWith(Object thisObject, Object[] args) throws Throwable {
        proceedArgs = args;
        return proceed();
    }
}
