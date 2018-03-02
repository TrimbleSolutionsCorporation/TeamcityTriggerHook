/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ut.com.trimble.tekla.teamcity;

import com.trimble.tekla.helpers.ExclusionTriggers;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


/**
 *
 * @author jocs
 */
public class ExclusionTriggersTest {
    
     
  @Test  
  public void TriggersIfInclusionAndExclusionIsEmpty()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");
    changedfilespath.add("dir_3/abc.txt");

    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("","", changedfilespath));    
  }
  
  @Test  
  public void TriggersIfInclusionIsFound()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");
    changedfilespath.add("dir_3/abc.txt");

    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("dir_1","", changedfilespath));    
  }
  
  @Test  
  public void TriggersIfBothInclusionAndExclusionAreTheSame()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");
    changedfilespath.add("dir_3/abc.txt");

    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("dir_1","dir_1", changedfilespath));    
  }
  
  @Test  
  public void TriggersIfOneInclusionIsFoundInChangesWithExclusion()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");
    changedfilespath.add("dir_3/abc.txt");

    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("dir_1","dir_2", changedfilespath));    
  }
  
  @Test  
  public void TriggersIfOneInclusionIsFoundInChangesButFirstMatchIsExclusion()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_2/abc.txt");
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_3/abc.txt");

    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("dir_1","dir_2", changedfilespath));    
  } 
  
  @Test
  public void NoTriggersIfInclusionIsFoundButNoFolder()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");
    changedfilespath.add("dir_3/abc.txt");

    assertEquals(false, ExclusionTriggers.ShouldTriggerOnListOfFiles("dir_4","", changedfilespath));    
  }

  @Test  
  public void TriggersIfFolderIsExcluded()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");

    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("", "dir_1", changedfilespath));    
  }
  
  @Test  
  public void NoTriggersIfMultipleFolderAreDefinedExcluded()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("dir_1/abc.txt");
    changedfilespath.add("dir_2/abc.txt");

    assertEquals(false, ExclusionTriggers.ShouldTriggerOnListOfFiles("", "dir_1,dir_2", changedfilespath));    
  }  
  
  @Test  
  public void TriggersIfFolderIsExcludedWithOnlyOneChange()  
  {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    changedfilespath.add("basic_branching/file - Copy (3) - Copy.txt");
    assertEquals(true, ExclusionTriggers.ShouldTriggerOnListOfFiles("", "add_file", changedfilespath));    
  }    
}
