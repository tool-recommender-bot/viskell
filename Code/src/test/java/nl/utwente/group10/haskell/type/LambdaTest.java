package nl.utwente.group10.haskell.type;

import static org.junit.Assert.*;

import java.util.Arrays;

import nl.utwente.group10.ghcj.HaskellException;
import nl.utwente.group10.haskell.env.Environment;
import nl.utwente.group10.haskell.env.HaskellCatalog;
import nl.utwente.group10.haskell.expr.Apply;
import nl.utwente.group10.haskell.expr.Binder;
import nl.utwente.group10.haskell.expr.Expression;
import nl.utwente.group10.haskell.expr.Lambda;
import nl.utwente.group10.haskell.expr.LocalVar;
import nl.utwente.group10.haskell.expr.Value;

import org.junit.Test;

public class LambdaTest {
    @Test
    public void testLambdaWrapping() throws HaskellException {
        Environment env = new HaskellCatalog().asEnvironment();
        
        // wrapping a simple function in a lambda
        Binder x = new Binder("x");
        Binder y = new Binder("y");
        Expression pxy = new Apply (new Apply(env.useFun("(+)"), new LocalVar(x)), new LocalVar(y));
        Expression add = new Lambda(Arrays.asList(x,y), pxy);
        Type tla = add.findType();
        assertEquals("Num a -> Num a -> Num a", tla.toHaskellType());
        
        // using the same binder twice
        Binder z = new Binder("z");
        Expression ezz = new Apply (new Apply(env.useFun("(^)"), new LocalVar(z)), new LocalVar(z));
        Expression exp = new Lambda(Arrays.asList(z), ezz);
        Type tle = exp.findType();
        assertEquals("Integral b -> Num a", tle.toHaskellType());

        Binder u = new Binder("u");
        Expression f5 = new Value(Type.con("Float"), "5.0");
        Expression l5 = new Lambda(Arrays.asList(u), f5);
        assertEquals("u -> Float", l5.findType().toHaskellType());
    }
    
    @Test
    public void testLambdaAnnotated() throws HaskellException {
        Environment env = new HaskellCatalog().asEnvironment();
        
        // wrapping a simple function in a lambda
        Binder x = new Binder("x", Type.con("Int"));
        Binder y = new Binder("y");
        Expression pxy = new Apply (new Apply(env.useFun("(+)"), new LocalVar(x)), new LocalVar(y));
        Expression add = new Lambda(Arrays.asList(x,y), pxy);
        Type tla = add.findType();
        assertEquals("Int -> Int -> Int", tla.toHaskellType());
        
        // using the same binder twice
        Binder z = new Binder("z", Type.var("r", env.lookupClass("RealFloat")));
        Expression ezz = new Apply (new Apply(env.useFun("(**)"), new LocalVar(z)), new LocalVar(z));
        Expression exp = new Lambda(Arrays.asList(z), ezz);
        Type tle = exp.findType();
        assertEquals("RealFloat a -> RealFloat a", tle.toHaskellType());

        Binder u = new Binder("u", Type.listOf(Type.con("Int")));
        Expression f5 = new Value(Type.con("Float"), "5.0");
        Expression l5 = new Lambda(Arrays.asList(u), f5);
        assertEquals("[Int] -> Float", l5.findType().toHaskellType());
    }

}