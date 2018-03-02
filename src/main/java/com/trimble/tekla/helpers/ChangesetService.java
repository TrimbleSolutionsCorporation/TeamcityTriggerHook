package com.trimble.tekla.helpers;

import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.content.AbstractChangeCallback;
import com.atlassian.bitbucket.content.Change;
import com.atlassian.bitbucket.content.ChangeCallback;
import com.atlassian.bitbucket.content.ChangeContext;
import com.atlassian.bitbucket.content.ChangeSummary;
import com.atlassian.bitbucket.content.ChangesRequest;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestChangesRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.Repository;
import java.io.IOException;

import java.util.*;
import javax.inject.Named;

@Named
public class ChangesetService {

  public static Iterable<String> GetChangedFiles(Iterable<Change> changes) {
    final ArrayList<String> changedfilespath = new ArrayList<>();
    for (final Change change : changes) {
      changedfilespath.add(change.getPath().toString());
    }
    return changedfilespath;
  }
  
  public static ArrayList<String> GetChangedFiles(final PullRequest pr, final PullRequestService pullRequestService) {
    final ArrayList<String> changedfilespath = new ArrayList<>();

    pullRequestService.streamChanges(new PullRequestChangesRequest.Builder(pr).build(), new ChangeCallback() {
      @Override
      public boolean onChange(Change change) throws IOException {
        changedfilespath.add(change.getPath().toString());
        return true;
      }

      @Override
      public void onEnd(ChangeSummary cs) throws IOException {
        // dont do anything.
      }

      @Override
      public void onStart(ChangeContext cc) throws IOException {
        // dont do anything.
      }
    });

    return changedfilespath;
  }
  
  public static ArrayList<String> GetChangedFiles(final CommitService scmService, final Repository repository, RefChange refChange) {
    final ArrayList<String> changedfilespath = new ArrayList<>();
 
    ChangesRequest changesRequest = new ChangesRequest.Builder(repository, refChange.getToHash()).sinceId(refChange.getFromHash()).build();
                 
    scmService.streamChanges(changesRequest, new ChangeCallback() {
        @Override
        public boolean onChange(Change change) throws IOException {
          changedfilespath.add(change.getPath().toString());
          return true;
        }

        @Override
        public void onEnd(ChangeSummary cs) throws IOException {
          //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onStart(ChangeContext cc) throws IOException {
          //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
      });
    
    return changedfilespath;
  }   
}
