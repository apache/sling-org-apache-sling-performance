/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.performance.tests;

import java.math.BigInteger;
import java.security.SecureRandom;
import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import junit.framework.Assert;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.performance.AbstractRepositoryTest;
import org.apache.sling.performance.TestHelper;
import org.apache.sling.performance.ResourceResolverTestRequest;
import org.junit.After;
import org.junit.Before;

class ResolveNonExistingWithManyVanityPathTest extends AbstractRepositoryTest {
    
    private static final String NT_VANITY_PATH = "sling:VanityPath";

    private static final String PN_VANITY_PATH = "sling:vanityPath";
    
    private final TestHelper helper;
    
    private Node mapRoot;
    
    private ResourceResolver resResolver;
    
    private Node rootNode;
    
    private String rootPath;

    private final int nodeCount;
    
    private final int childNodeCount;
    
    public ResolveNonExistingWithManyVanityPathTest(TestHelper helper, int nodeCount, int childNodeCount) {
        this.helper = helper;
        this.nodeCount = nodeCount;
        this.childNodeCount = childNodeCount;
    }

    @After
    protected void afterSuite() throws Exception {
        if (helper != null) {
            helper.dispose();
        }

        if (rootNode != null) {
            rootNode.remove();
        }
        if (mapRoot != null) {
            mapRoot.remove();
        }
        session.save();
    }

    @Before
    protected void beforeSuite() throws Exception {
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/folder.cnd"));
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/resource.cnd"));
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/vanitypath.cnd"));
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/mapping.cnd"));

        // test data
        rootPath = "/test" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1), "nt:unstructured");

        // test mappings
        mapRoot = getSession().getRootNode().addNode("etc", JcrConstants.NT_FOLDER);
        Node map = mapRoot.addNode("map", "sling:Mapping");
        Node http = map.addNode("http", "sling:Mapping");
        http.addNode("localhost.80", "sling:Mapping");
        Node https = map.addNode("https", "sling:Mapping");
        https.addNode("localhost.443", "sling:Mapping");

        SecureRandom random = new SecureRandom();

        // creating <nodeCount> x <childNodeCount> nodes with vanity
        for (int j = 0; j < nodeCount; j++) {
            Node content = rootNode.addNode("a" + j, JcrConstants.NT_UNSTRUCTURED);
            String s = new BigInteger(130, random).toString(32);
            content.addMixin(NT_VANITY_PATH);
            content.setProperty(PN_VANITY_PATH, s);
            for (int k = 0; k < childNodeCount; k++) {
                Node content2 = content.addNode("b" + k, JcrConstants.NT_UNSTRUCTURED);
                String ss = new BigInteger(130, random).toString(32);
                content2.addMixin(NT_VANITY_PATH);
                content2.setProperty(PN_VANITY_PATH, ss);
            }
            if (j % 10 == 0) {
                session.save();
            }
        }

        session.save();
        
        helper.init(rootPath, session, getRepository());

        resResolver = helper.getResourceResolver();

    }


    protected void runTest() throws Exception {
        String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath) + "/" + "testNonExistingVanity"
                + ".print.html");
        HttpServletRequest request = new ResourceResolverTestRequest(path);
        Resource res = resResolver.resolve(request, path);
        Assert.assertNotNull(res);
    }

}
