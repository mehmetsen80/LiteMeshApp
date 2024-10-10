//package org.lite.gateway.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
////NOT USED RIGHT NOW, KEEP FOR THE FUTURE
//@Service
//public class SharedErrorContext {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    public void storeError(String requestId, Throwable error) {
//        redisTemplate.opsForValue().set(requestId, error, 5, TimeUnit.MINUTES); // store error for 5 minutes
//    }
//
//    public Throwable retrieveError(String requestId) {
//        return (Throwable) redisTemplate.opsForValue().get(requestId);
//    }
//
//    public void removeError(String requestId) {
//        redisTemplate.delete(requestId);
//    }
//}
//
