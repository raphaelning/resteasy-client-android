package org.jboss.resteasy.skeleton.key.as7;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.as.web.security.JBossGenericPrincipal;
import org.jboss.security.NestableGroup;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaSecurityContextHelper
{
   public GenericPrincipal createPrincipal(Realm realm, Principal identity, Collection<String> roleSet)
   {
      Subject subject = new Subject();
      String credentials = "";
      Set<Principal> principals = subject.getPrincipals();
      principals.add(identity);
      Group[] roleSets = getRoleSets(roleSet);
      for(int g = 0; g < roleSets.length; g ++)
      {
         Group group = roleSets[g];
         String name = group.getName();
         Group subjectGroup = createGroup(name, principals);
         if( subjectGroup instanceof NestableGroup)
         {
            /* A NestableGroup only allows Groups to be added to it so we
            need to add a SimpleGroup to subjectRoles to contain the roles
            */
            SimpleGroup tmp = new SimpleGroup("Roles");
            subjectGroup.addMember(tmp);
            subjectGroup = tmp;
         }
         // Copy the group members to the Subject group
         Enumeration<? extends Principal> members = group.members();
         while( members.hasMoreElements() )
         {
            Principal role = (Principal) members.nextElement();
            subjectGroup.addMember(role);
         }
      }
      // add the CallerPrincipal group if none has been added in getRoleSets
      Group callerGroup = new SimpleGroup(SecurityConstants.CALLER_PRINCIPAL_GROUP);
      callerGroup.addMember(identity);
      principals.add(callerGroup);
      SecurityContext sc = SecurityContextAssociation.getSecurityContext();
      Principal userPrincipal = getPrincipal(subject);
      sc.getUtil().createSubjectInfo(userPrincipal, credentials, subject);
      List<String> rolesAsStringList = new ArrayList<String>();
      rolesAsStringList.addAll(roleSet);
      return  new JBossGenericPrincipal(realm, userPrincipal.getName(), null, rolesAsStringList,
              userPrincipal, null, credentials, null, subject);

   }
   /**
    * Get the Principal given the authenticated Subject. Currently the first principal that is not of type {@code Group} is
    * considered or the single principal inside the CallerPrincipal group.
    *
    * @param subject
    * @return the authenticated principal
    */
   protected Principal getPrincipal(Subject subject) {
      Principal principal = null;
      Principal callerPrincipal = null;
      if (subject != null) {
         Set<Principal> principals = subject.getPrincipals();
         if (principals != null && !principals.isEmpty()) {
            for (Principal p : principals) {
               if (!(p instanceof Group) && principal == null) {
                  principal = p;
               }
               if (p instanceof Group) {
                  Group g = Group.class.cast(p);
                  if (g.getName().equals(SecurityConstants.CALLER_PRINCIPAL_GROUP) && callerPrincipal == null) {
                     Enumeration<? extends Principal> e = g.members();
                     if (e.hasMoreElements())
                        callerPrincipal = e.nextElement();
                  }
               }
            }
         }
      }
      return callerPrincipal == null ? principal : callerPrincipal;
   }

   protected Group createGroup(String name, Set<Principal> principals)
   {
      Group roles = null;
      Iterator<Principal> iter = principals.iterator();
      while( iter.hasNext() )
      {
         Object next = iter.next();
         if( (next instanceof Group) == false )
            continue;
         Group grp = (Group) next;
         if( grp.getName().equals(name) )
         {
            roles = grp;
            break;
         }
      }
      // If we did not find a group create one
      if( roles == null )
      {
         roles = new SimpleGroup(name);
         principals.add(roles);
      }
      return roles;
   }

   protected Group[] getRoleSets(Collection<String> roleSet)
   {
      SimpleGroup roles = new SimpleGroup("Roles");
      Group[] roleSets = {roles};
      for (String role : roleSet)
      {
         roles.addMember(new SimplePrincipal(role));
      }
      return roleSets;
   }

}
