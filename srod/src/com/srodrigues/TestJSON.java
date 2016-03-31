package com.srodrigues;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import com.srodrigues.srod.json.JSONReader;
import com.srodrigues.srod.json.JSONWriter;
import com.srodrigues.srod.layer.java.Reflection;

public class TestJSON {

   public static class Flight {
      protected String number;

      public String getNumber() {
         return number;
      }

      public void setNumber(String number) {
         this.number = number;
      }

      public Passenger[] getPassengers() {
         return passengers;
      }

      protected Passenger[] passengers = new Passenger[0];

      public Flight() {}

      @Override
      public String toString() {
         return "Flight(" + number + ")";
      }

      public void addPassengers(Passenger p2) {
         passengers = Arrays.copyOf(passengers, passengers.length + 1);
         passengers[passengers.length - 1] = p2;
      }
   }

   public static class Passenger {
      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public Flight getFlight() {
         return flight;
      }

      public void setFlight(Flight flight) {
         this.flight = flight;
      }

      protected String name;

      protected Flight flight;

      public Passenger() {}

      @Override
      public String toString() {
         return "Passenger(" + name + ")";
      }

   }

   public static void main(String[] args) {
      Flight f1 = new Flight();
      f1.setNumber("1234");

      Flight f2 = new Flight();
      f2.setNumber("4321");

      Passenger p1 = new Passenger();
      p1.setName("Ana");

      Passenger p2 = new Passenger();
      p2.setName("Joao");

      Passenger p3 = new Passenger();
      p3.setName("Pedro");

      Passenger p4 = new Passenger();
      p4.setName("Carla");

      f1.addPassengers(p1);
      f1.addPassengers(p2);
      f1.addPassengers(p3);
      f1.addPassengers(p4);

      f2.addPassengers(p4);
      f2.addPassengers(p3);
      f2.addPassengers(p2);
      f2.addPassengers(p1);
      p1.flight = f2;
      p3.flight = f2;

      p2.flight = f1;
      p4.flight = f1;

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PrintStream ps = new PrintStream(baos);

      JSONWriter json = new JSONWriter(ps);
      try {
         json.write(f1);
      } catch (IOException e) {
         e.printStackTrace();
      }

      String string = baos.toString();
      System.out.println("JSONWRITE:\r\n" + string);

      JSONReader reader = new JSONReader(string);
      Object o = null;
      try {
         o = reader.read();
      } catch (IOException e) {
         e.printStackTrace();
      }
      System.err.println("JSONREAD:\r\n" + o);

      final Flight flight = (Flight) Reflection.convert(Flight.class, o);

      System.err.println(flight);
      System.err.println(flight.passengers[3]);
   }

}
