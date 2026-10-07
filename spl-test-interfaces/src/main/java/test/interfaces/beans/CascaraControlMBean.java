package test.interfaces.beans;

import javax.management.JMException;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public interface CascaraControlMBean {

    void loadModule() throws JMException;
    void unloadModule() throws JMException;

    void test();

    void exit();

    void setLogLevel(Level level);
}