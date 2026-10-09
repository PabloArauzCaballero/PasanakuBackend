package bo.aportaya.nucleofinanciero;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Le pone a un caso de uso construido a mano el mismo proxy transaccional que le pondria
 * Spring: sin el, {@code @Transactional} no abre nada y {@code Datos} se niega a operar.
 */
final class TransaccionalDePrueba {
    private TransaccionalDePrueba() {}

    @SuppressWarnings("unchecked")
    static <T> T conTransaccion(T objetivo, PlatformTransactionManager manager) {
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(objetivo);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(interceptor);
        return (T) proxy.getProxy();
    }
}
