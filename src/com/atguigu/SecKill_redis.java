package com.atguigu;

import java.io.IOException;
import java.util.List;

import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

public class SecKill_redis {
	
	private static final  org.slf4j.Logger logger =LoggerFactory.getLogger(SecKill_redis.class) ;

	public static void main(String[] args) {
		Jedis jedis =new Jedis("192.168.192.10",6379);

		System.out.println(jedis.ping());
	 
		jedis.close();
	}
	
	public static boolean doSecKill(String uid,String prodid) throws IOException {
		//通过连接池，解决连接超时问题
		JedisPool jedisPool = JedisPoolUtil.getJedisPoolInstance();
		
		Jedis jedis =jedisPool.getResource();//如果jedis是从连接池获取的，当执行close时回收到连接池中，不会关闭
		//1.获取redis存储相应key
		
		String qtKey="sk:"+prodid+":qt";
		String usrKey="sk:"+prodid+":usr";
		
		//判断用户是否秒杀成功过
		if(jedis.sismember(usrKey, uid)){
			System.out.println("已经秒杀过！！！");
			jedis.close();
			return false;
		}
		
		jedis.watch(qtKey);
		//对商品判断
		String qtValue=jedis.get(qtKey);
		if(qtValue==null){
			System.out.println("商品没有初始化！！！");
			jedis.close();
			return false;
		}else{
			int qt=Integer.parseInt(qtValue);
			if(qt==0){
				System.out.println("秒杀失败！");
				jedis.close();
				return false;
			}
		}
		Transaction t=jedis.multi();
		//2.商品减库存
		t.decr(qtKey);
		//3.增加相应用户
		t.sadd(usrKey, uid);
		List<Object> list = t.exec();
		if(list.size()==0 ||list==null){
			System.out.println("排队失败！！！");
			jedis.close();
			return false;
		}
		System.out.println("秒杀成功！！！");
		jedis.close();
		return true;
	}

}
