# odbogm
# OrientDB Object to Graph Mapper 

ODBOGM is a clear Java Object to Graph Vertex to the ***OrientDB*** database. I really miss the DB4O database. I have not see nothing similar to that, but DB4O is dead so I want to have the same functionality over another database. I have choose OrientDB as a target so I want to make a mapper it be as noninvasive as possible to the developer.

This is my first approach and is in ***experimental version***. Use it as your own risk.
The ODBOGM work against the Graph DB API of the OrientDB. It let you to access the underlying DB if you want but it's implement the basic function to work with the database in an absolutely clear way like DB4O.

To start using it you must initialize a ***SessionManager*** and set the URL to connect to the DB:
```Java
SessionManager sm = new SessionManager("remote:localhost/Test", "root", "toor");
```
After that, the SessionManager implements the method to:

* ***begin***: init the communication and start the first trasaction.
* ***store***: add a new record to the database.
* ***get***: retrieve a record from the database.
* ***delete***: delete the record from the database. 
* ***commit***: process the current transaction.
* ***rollback***: rollback the current transaction. TBD
* ***query***: query the database.
* ***shutdown***: shutdown the DB communication.
* ***setAuditOnUser***: set the user name to store in auditlog. Read ***Auditory*** section.

So, for example, let say that we have this class definition:

```Java
public class Ex1 {
    private final static Logger LOGGER = Logger.getLogger(Ex1.class .getName());
    private int i = 0;
    public int x = 10;
    public Ex1() {
    }
    
    public int inc(){
        return ++i;
    }
    
    public int test() {
        return x + i;
    }
}
```

to store an instance you do:

```Java
Ex1 ex1 = new Ex1();
sm.store(ex1);
sm.commit();
```

Every primitive field type and String are stored directly to the DB including private field in a Vertex.
If the object has List or Maps of primitive type, they are mapped as a embedded list/maps.
Every Object is mapped to a Vertex and an Edge is created with a label <Class>_<field>. The field will not be
created as a part of the class.
The ***final static*** fields are ignored so take care about this.

Example:
```Java
public class Ex2 extends Ex1 {
    private final static Logger LOGGER = Logger.getLogger(Ex2.class .getName());
    
    private Ex1 inner;
    private String ex2String;
    
    
    public Ex2() {
        inner = new Ex1();
        ex2String = "hola mundo";
    }

    public Ex1 getInner() {
        return inner;
    }

    public void setInner(Ex1 inner) {
        this.inner = inner;
    }

    public String getEx2String() {
        return ex2String;
    }

    public void setEx2String(String ex2String) {
        this.ex2String = ex2String;
    }
    
}

```
Here, the field ***inner*** is not a primitive type. OrientDB let us store it as an embedded object or we can tell the ODBOGM to store it as a new vertex related to the main object.
Currently, the embedded alternative is not implemented. Every nonprimitive field is mapped as a Vertex except it is annotated with @Ignore.
***Collencions*** and ***Map*** have been subclassed to allow the communication to the DB in a transparent way. At this time only ***ArrayList***, ***Vector***, ***LinkedList*** and ***HashMap*** are implemented.  

We must annotate the field with:
* ***@Ignore***: to skip the field. Useful to ignore the Logger field for example.

so, in the last class we must add this annotation to the fields:
```Java
public class Ex2 extends Ex1 {
    @Ignore // final static field are ignored in any case but a warning is throw if it not annotaded.
    private final static Logger LOGGER = Logger.getLogger(Ex2.class .getName());
    
    private Ex1 inner;
    private String ex2String;
    ….
}
```
Now, the field ***inner*** will be mapped to a new vertex of class ***Ex1*** and the edge that link the current vertex with the new vertex will be labeled with the ***classname + _ + fieldname***. In this case, the resulting graph will be:
```Java
(Ex2)  ← Ex2_inner → (Ex1)
```
Where (Ex2) is a Vertex of class Ex2. The (Ex2) vertex will have one atribute called ex2String stored in it.

Now, if we have a collection inside the object, every element of the collection is mapped in a vertex and in the same way, a Edge is created for every related element. The exception is if the collection contains only primitives. In this case are mapped as embedded. (Ej: ArrayList<String> col ...) 

```Java
public class Ex2 extends Ex1 {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(Ex2.class .getName());
    
    private Ex1 inner;
    private String ex2String;
   

    public ArrayList<Ex1> alEX;

    ….
}
```
in this case, the resulting graph is:

```Java
(Ex2) ---- Ex2_alEx -- > (Ex1)
      |--- Ex2_alEx -- > (Ex1)
      |--- Ex2_alEx -- > (Ex1)
       ….
```
If the collection is a ***Map***, the strategy is the same but the key value are stored as a field in the edge. The key could be an object to but this object must be simple.
At this time only ***HashMap*** is implemented.

## Getting Objects.
The most simple way to retrieve an object is the use of the ***get*** method. To use it we must pass the class to map to the vertex and the vertex RID.
Example:
```Java
Ex1 ex1 = sm.get(Ex1.class, "#12:123");
```
This will return an object with all it field filled with the data stored in the #12:123 vetex. The object load every field and ***@LinkList*** lazy.

During a transaction, the SessionManager will return always the same reference to the object if it was previously returned. When transaction is closed due to a commit/rollback call, the object cache is reseted and a new reference is returned.

## Deleting Objects.
To delete an object that was ***previous retrieved*** from the DB, we call 
```Java
sm.delete(Object o);
```
Since an object could be divided into multiple vertex we must tell what to do in this case. The annotation ***@CascadeDelete*** over a field do the job.
If the vertex holding the object have more than one reference, that indicate that the object is part of another object and if it is deleted, a referential integrity could be occurs, so an exception is throws to catch that problem.


## Rollback
Once we have connected to the DB, a transaction is open. Every modification since the previous commit is rolledback.
```Java
sm.rollback();
```

## Querying
Al query 
To query the database there are some custom implementations.
A direct query:
```Java
sm.query(String sql);
```
In this case, the full string is passed to the underlying database and the result is returned.
```Java
sm.query(Class<T> clase);
```
In this case, the query will return a List with all vertex of class <T> mapped in objects. In the db is mapped as: “select from ”+clase.getSimpleName()
```Java
sm.query(Class<T> clase, String body);
```
This query is similar but let us add a body after the select-from clause.

## Auditory
The SessionManager implement a way to audit the dialog with the database. Since we need a more complex system of security I decided to implement a diferent way of manage users. We need to control the user right at application level and we want to not restrict the communication with the DB, so the SM let you to stablish a connection with one user (we are using the ***writer*** profile) and after that set an audit user name that you get from your app. 
For example:

```Java
sm = new SessionManager("remote:localhost/somedb", "writer", "w1t3r");
sm.begin();
sm.setAuditOnUser("jlennon");
```

The ***sm*** will create a class called ***ODBAuditLog*** with this fields:

* ***timestamp***: the timestamp of the log.
* ***user***: the user set with ***setAuditOnUser***
* ***rid***: record id of the vertex accessed.
* ***transactionID***: and UUID that is common to the whole transaction.
* ***label***: action that has throw the audit log with some data.
* ***action***: is the pure action:  1 = read, 2 = write, 4 = delete.
* ***log***: in this field is the data of the vertex/edge in the state at that moment.

To audit a object, just annotate it with 

```Java
@Audit(log = Audit.AuditType.<READ | WRITE | DELETE | ALL>)
```

for example, in the previous code you can set audit on every action in this way:

```Java
@Audit(log = Audit.AuditType.ALL)
public class Ex2 extends Ex1 {
    ...
}
```

After that, the ***sm*** will log every access to the instance of Ex2 class. 


----


Working on the security system……

