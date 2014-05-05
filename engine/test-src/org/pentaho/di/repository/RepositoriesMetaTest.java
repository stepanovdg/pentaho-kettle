/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2014 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.di.repository;

import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.KettleLogStore;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;


/**
 * User: Dzmitry Stsiapanau Date: 4/2/14 Time: 1:43 PM
 */
public class RepositoriesMetaTest {
  @Test
  public void testReadData() throws Exception {
    //KettleEnvironment.init();
    KettleClientEnvironment.init();
    //KettleLogStore.init( 5000, 1440 );
    final AtomicInteger atomicInteger = new AtomicInteger( 4 );
    final AtomicBoolean failed = new AtomicBoolean( false );
    final AtomicBoolean start = new AtomicBoolean( false );
    for ( int i = 0; i < atomicInteger.get(); i++ ) {
      new Thread( new Runnable() {
        @Override public void run() {
          try {
            RepositoriesMeta rm = new RepositoriesMeta();
            while ( !start.get() ) {
            }
            rm.readData();
          } catch ( Exception e ) {
            e.printStackTrace();
            failed.set( true );
          } finally {
            atomicInteger.decrementAndGet();
          }
        }
      } ).start();
    }
    start.set( true );
    while ( atomicInteger.get() != 0 ) {
    }
    assertFalse( "Failed", failed.get() );
  }

  @Test
  public void testRace() throws Exception {
    final AtomicBoolean failed = new AtomicBoolean( false );
    final AtomicInteger atomicInteger = new AtomicInteger( 2 );
    final String prevUserDir = System.getProperty( "user.dir" );
    final String separator = System.getProperty( "file.separator" );
    System.setProperty( "user.dir", prevUserDir + separator + "dist" );

    final File logDir = new File( prevUserDir + separator + "dist" + separator + "logs" );

    logDir.mkdir();
    //System.out.println( logDir.getAbsolutePath() );
    for ( int i = 0; i < atomicInteger.get(); i++ ) {
      new Thread( new Runnable() {
        @Override public void run() {
          try {
            File log = File.createTempFile( "kitchenlog", ".txt", logDir );
            Process pr = Runtime.getRuntime().exec(
              "cmd /c " + prevUserDir + separator + "dist" + separator
                + "Kitchen.bat /rep=mysql_pentaho /user=Admin /pass=admin /job=j1 /level=Debug /logfile=" + log
                .getAbsolutePath() + " >> " + log.getAbsolutePath() + "1" );
            int exitValue = pr.waitFor();
            if ( exitValue != 0 ) {
              failed.set( true );
              System.err.println( exitValue + "=exitValue" + log.getPath() );
            } else {
              log.deleteOnExit();
              new File( log.getAbsolutePath() + "1" ).deleteOnExit();
            }
          } catch ( Exception e ) {
            e.printStackTrace();
            failed.set( true );
          } finally {
            atomicInteger.decrementAndGet();
          }
        }
      } ).start();
      //System.out.println(i);
    }
    while ( atomicInteger.get() != 0 && !failed.get() ) {
    }
    assertFalse( "Failed", failed.get() );
    System.setProperty( "user.dir", prevUserDir );

  }
}
