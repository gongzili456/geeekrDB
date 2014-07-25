package test.geeekr.db;

import org.junit.Test;

import com.geeekr.db.Inflector;

public class TestInflector {

    @Test
    public void underline(){
      System.out.println(Inflector.getInstance().tableize("GeekrPost"));
    }
}
