/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class StandardDITest
{
   public static class TestedClass
   {
      @Inject static Runnable globalAction;

      private final Collaborator collaborator;
      @Inject private Collaborator collaborator1;
      Collaborator collaborator2;
      @Inject int someValue;
      @Inject private int anotherValue;

      @Inject public TestedClass(Collaborator collaborator) { this.collaborator = collaborator; }

      @SuppressWarnings("UnusedParameters")
      public TestedClass(Collaborator collaborator, int anotherValue) { throw new RuntimeException("Must not occur"); }
   }

   class Collaborator { boolean b = true; }

   @Tested TestedClass tested1;
   @Injectable Collaborator collaborator; // for constructor injection
   @Injectable("123") int someValue;
   @Injectable final int anotherValue = 45;

   static final class TestedClassWithNoAnnotatedConstructor
   {
      @Inject int value;
      @Inject String aText;
      String anotherText;
   }

   @Tested TestedClassWithNoAnnotatedConstructor tested2;
   @Injectable final String text1 = "Abc";

   public static class TestedClassWithInjectOnConstructorOnly
   {
      String name;
      @Inject public TestedClassWithInjectOnConstructorOnly() {}
   }

   @Tested TestedClassWithInjectOnConstructorOnly tested3;

   @Test
   public void invokeInjectAnnotatedConstructorOnly()
   {
      assertSame(collaborator, tested1.collaborator);
      assertNull(tested1.collaborator1);
      assertNull(tested1.collaborator2);
      assertEquals(123, tested1.someValue);
      assertEquals(45, tested1.anotherValue);

      assertEquals(123, tested2.value);
   }

   @Test
   public void assignInjectAnnotatedFieldsWhileIgnoringNonAnnotatedOnes(
      @Injectable Collaborator collaborator2, @Injectable Collaborator collaborator1, @Injectable("67") int notToBeUsed)
   {
      assertSame(collaborator, tested1.collaborator);
      assertSame(collaborator1, tested1.collaborator1);
      assertNull(tested1.collaborator2);
      assertEquals(123, tested1.someValue);
      assertEquals(45, tested1.anotherValue);

      assertEquals(123, tested2.value);
   }

   @Test
   public void assignAnnotatedFieldEvenIfTestedClassHasNoAnnotatedConstructor(@Injectable("123") int value)
   {
      assertEquals(123, tested2.value);
   }

   @Injectable Runnable action;

   @Test
   public void assignAnnotatedStaticFieldDuringFieldInjection()
   {
      assertSame(action, TestedClass.globalAction);
   }

   @Test
   public void onlyConsiderAnnotatedFieldsForInjection(@Injectable("XY") String text2)
   {
      assertEquals(text1, tested2.aText);
      assertNull(tested2.anotherText);
      assertNull(tested3.name);
   }

   static final class TestedClassWithProviders
   {
      final int port;
      final Collaborator collaborator;
      @Inject Provider<String> user;
      @Inject Provider<String> password;

      @Inject TestedClassWithProviders(Provider<Integer> port, Collaborator collaborator)
      {
         this.port = port.get();
         this.collaborator = collaborator;
      }
   }

   @Tested TestedClassWithProviders tested4;
   @Injectable Integer portNumber = 4567;
   @Injectable String user = "John";
   @Injectable String password = "123";

   @Test
   public void supportProviderFieldsAndParameters()
   {
      assertEquals(portNumber.intValue(), tested4.port);
      assertSame(collaborator, tested4.collaborator);
      assertEquals(user, tested4.user.get());
      assertEquals(password, tested4.password.get());
   }

   static final class TestedClassWithVarargsParameterForProviders
   {
      final Collaborator collaborator1;
      final Collaborator collaborator2;
      final List<Collaborator> optionalCollaborators = new ArrayList<Collaborator>();
      @Inject Provider<String> nameProvider;

      @Inject TestedClassWithVarargsParameterForProviders(Provider<Collaborator>... collaborators)
      {
         int n = collaborators.length;
         assertTrue(n > 1);

         collaborator1 = collaborators[0].get();
         assertSame(collaborator1, collaborators[0].get()); // default (singleton)

         collaborator2 = collaborators[1].get();
         assertNull(collaborators[1].get()); // recorded

         if (n > 2) {
            Collaborator col = collaborators[2].get();
            optionalCollaborators.add(col);
         }
      }
   }

   @Tested TestedClassWithVarargsParameterForProviders tested5;
   @Injectable Provider<Collaborator> collaboratorProvider;
   @Injectable Collaborator col3;

   @Before
   public void configureProviderUsedByConstructorOfTestedClass()
   {
      new NonStrictExpectations() {{
         collaboratorProvider.get(); result = col3; result = null;
      }};
   }

   @Test
   public void supportVarargsParameterWithProviders(@Injectable final Provider<String> nameProvider)
   {
      final String[] names = {"John", "Mary"};
      new Expectations() {{ nameProvider.get(); result = names; }};

      assertSame(collaborator, tested5.collaborator1);
      assertNotNull(tested5.collaborator2);
      assertNotSame(tested5.collaborator1, tested5.collaborator2);
      assertEquals(Arrays.asList(col3), tested5.optionalCollaborators);

      assertEquals(names[0], tested5.nameProvider.get());
      assertEquals(names[1], tested5.nameProvider.get());
   }
}
