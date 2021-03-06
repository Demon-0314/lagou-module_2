package com.lagou.sqlsession;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MapperStatement;
import java.beans.IntrospectionException;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.List;

/**
 * @ClassName DefaultSqlSession
 * @Description TODO
 * @Author 智弘
 * @Date 2020/11/22 23:42
 * @Version 1.0
 */
public class DefaultSqlSession implements SqlSession {
    private Configuration configuration;

    private Executor executor = new SimpleExecutor();

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> selectList(String statementid, Object... params) throws IllegalAccessException, IntrospectionException, InstantiationException, NoSuchFieldException, SQLException, InvocationTargetException, ClassNotFoundException {
        // 将要去完成对simpleExecutor里的query方法的调用
        SimpleExecutor simpleExecutor = new SimpleExecutor();
        MapperStatement mapperStatement = configuration.getMapperStatementMap().get(statementid);
        List<E> list = simpleExecutor.query(configuration, mapperStatement, params);
        return list;
    }

    @Override
    public <T> T selectOne(String statementid, Object... params) throws IllegalAccessException, ClassNotFoundException, IntrospectionException, InstantiationException, SQLException, InvocationTargetException, NoSuchFieldException {
        List<Object> objects = selectList(statementid, params);
        if (objects.size() == 1) {
            return (T) objects.get(0);
        } else {
            throw new RuntimeException("查询结果为空或者返回结果过多");
        }
    }


    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        // 使用JDK动态代理来为Dao接口生成动态代理,并返回
        Object proxyInstance = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(), new Class[]{mapperClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 1.组装方法入参
                String methodName = method.getName(); // 方法名
                String ClassName = method.getDeclaringClass().getName(); // 权限类名
                String statementId = ClassName + "." + methodName;
                if ("updateUser".equals(getCrudType(statementId))) {
                    return update(statementId, args);
                } else if ("deleteUser".equals(getCrudType(statementId))) {
                    return delete(statementId, args);
                } else if ("addUser".equals(getCrudType(statementId))) {
                    return addUser(statementId, args);
                } else {
                    // 获取被调用返回值类型
                    Type genericReturnType = method.getGenericReturnType();
                    // 判断是否进行了泛型类型参数化
                    if (genericReturnType instanceof ParameterizedType) {
                        List<Object> objects = selectList(statementId, args);
                        return objects;
                    }
                    return selectOne(statementId, args);
                }
            }
        });
        return (T) proxyInstance;
    }

    @Override
    public int addUser(String statementid, Object... params) throws ClassNotFoundException, SQLException, IllegalAccessException, NoSuchFieldException {
        return update(statementid, params[0]);
    }

    @Override
    public int update(String statementid, Object... params) throws ClassNotFoundException, SQLException, NoSuchFieldException, IllegalAccessException {
        MapperStatement mapperStatement = getMapperStatement(statementid);
        return executor.update(configuration, mapperStatement, params);
    }

    @Override
    public int delete(String statementid, Object... params) throws ClassNotFoundException, SQLException, IllegalAccessException, NoSuchFieldException {
        return update(statementid, params[0]);
    }

    /**
     * 获取 MapperStatement 的 CrudType
     *
     * @param statmentId
     * @return
     */
    private String getCrudType(String statmentId) {
        return getMapperStatement(statmentId).getId();
    }

    /**
     * 根据 statementid 获取 MapperStatement
     *
     * @param statementid
     * @return
     */
    private MapperStatement getMapperStatement(String statementid) {
        return configuration.getMapperStatementMap().get(statementid);
    }
}
