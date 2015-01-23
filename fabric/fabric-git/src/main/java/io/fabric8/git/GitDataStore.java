/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.git;

import io.fabric8.api.GitContext;
import io.fabric8.git.internal.GitOperation;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.PushResult;

public interface GitDataStore {

    Git getGit();
    
    Iterable<PushResult> doPush(Git git, GitContext context) throws Exception;
    
    <T> T gitOperation(GitContext context, GitOperation<T> operation, PersonIdent personIdent);
}
