这是一个基于 dbutils 的JDBC简单封装，定义了通用查询类QueryHelper，和POJO的父类Model。



####使用：

```java
@Entity
@Table(name = "users")
public class User extends Model implements Serializable{

  	@Id
	@Column(name = "id", nullable = false, unique = true)
	private Integer id;
	
	@Column(name = "name", nullable = false)
	private String name;
	
	.......
}

```

```java
@Test
public void insert() {
  User u = new User();
  u.setName("Jack");
  u.save();
}

```

```java
@Test
public void query() {
  User u = new User();
  u.query(1L);
  System.out.println(u);
}
```
