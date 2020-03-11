package org.jboss.resteasy.plugins.validation;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;

import org.hibernate.validator.internal.engine.path.NodeImpl;
import org.hibernate.validator.internal.metadata.descriptor.ConstraintDescriptorImpl;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright Jul 18, 2015
 */
public class SimpleViolationsContainer extends org.jboss.resteasy.api.validation.SimpleViolationsContainer implements Serializable
{
   private static final long serialVersionUID = -7895854137980651539L;

   public SimpleViolationsContainer(final Object target)
   {
      super(target);
   }

   public SimpleViolationsContainer(final Set<ConstraintViolation<Object>> cvs)
   {
      super(cvs);
   }

   /**
    * If some ConstraintViolations are created by Resteasy and some are created by CDI, two
    * essentially identical ones might appear to be different. For example, those created by Resteasy
    * might refer to CDI proxy classes while those created by CDI might refer to the backing java classes.
    */
   @Override
   public void addViolations(Set<ConstraintViolation<Object>> cvs)
   {
      if (cvs.size() == 0)
      {
         return;
      }
      if (getViolations().size() == 0)
      {
         getViolations().addAll(cvs);
         return;
      }
      boolean skip = false;
      for (ConstraintViolation<Object> cv1 : cvs)
      {
         skip = false;
         for (ConstraintViolation<Object> cv2 : getViolations())
         {
            if (compareConstraintViolation(cv1, cv2))
            {
               skip = true;
               break;
            }
         }
         if (!skip)
         {
             getViolations().add(cv1);
         }
      }
   }

   static boolean compareConstraintViolation(ConstraintViolation<?> cv1, ConstraintViolation<?> cv2)
   {
      if (cv1 == cv2)
      {
         return true;
      }

      if (cv1 == null || cv2 == null)
      {
         return false;
      }

      if (cv1.getMessage() != null ? !cv1.getMessage().equals(cv2.getMessage()) : cv2.getMessage() != null)
      {
         return false;
      }

      if (cv1.getPropertyPath() != null ? !comparePropertyPath(cv1.getPropertyPath(), cv2.getPropertyPath()) : cv2.getPropertyPath() != null)
      {
         return false;
      }

      // Can't compare root beans: one might be a proxy while the other one isn't.
      // Can't compare leaf bean instance: one might be a proxy while the other isn't.
      // Compare classes as an approximation.

      if (cv1.getRootBeanClass() != null ? !compareRootBeanClass(cv1, cv2) : cv2.getRootBeanClass() != null)
      {
         return false;
      }

      if (cv1.getLeafBean() != null ? !compareLeafBeanClass(cv1, cv2) : cv2.getLeafBean() != null)
      {
         return false;
      }

      if (cv1.getConstraintDescriptor() != null ? !cv1.getConstraintDescriptor().equals(cv2.getConstraintDescriptor()) : cv2.getConstraintDescriptor() != null)
      {
         return false;
      }

      if (cv1.getConstraintDescriptor() instanceof ConstraintDescriptorImpl && cv2.getConstraintDescriptor() instanceof ConstraintDescriptorImpl)
      {
         ConstraintDescriptorImpl<?> cdi1 = (ConstraintDescriptorImpl<?>) cv1.getConstraintDescriptor();
         ConstraintDescriptorImpl<?> cdi2 = (ConstraintDescriptorImpl<?>) cv2.getConstraintDescriptor();
         if (cdi1.getConstraintLocationKind() != null ? !cdi1.getConstraintLocationKind().equals(cdi2.getConstraintLocationKind()) : cdi2.getConstraintLocationKind() != null) {
            return false;
         }
      }
      if (cv1.getMessageTemplate() != null ? !cv1.getMessageTemplate().equals(cv2.getMessageTemplate()) : cv2.getMessageTemplate() != null)
      {
         return false;
      }
      // Compared above.
//      if (cv1.getRootBeanClass() != null ? !compareClass(cv1.getRootBeanClass(), cv2.getRootBeanClass()) : cv2.getRootBeanClass() != null)
//      {
//         return false;
//      }

      if (cv1.getInvalidValue() != null ? !cv1.getInvalidValue().equals(cv2.getInvalidValue()) : cv2.getInvalidValue() != null)
      {
         return false;
      }
      return true;
   }

   private static boolean compareRootBeanClass(ConstraintViolation<?> cv1, ConstraintViolation<?> cv2)
   {
      Class<?> c1 = cv1.getRootBeanClass();
      while ((c1.isSynthetic() || isEJBProxy(c1)) && !Object.class.equals(c1))
      {
         c1 = c1.getSuperclass();
      }
      Class<?> c2 = cv2.getRootBeanClass();
      while ((c2.isSynthetic() || isEJBProxy(c2)) && !Object.class.equals(c2))
      {
         c2 = c2.getSuperclass();
      }
      return c1.equals(c2);
   }

   private static boolean compareLeafBeanClass(ConstraintViolation<?> cv1, ConstraintViolation<?> cv2)
   {
      Object o1 = cv1.getLeafBean();
      Class<?> c1 = o1.getClass();
      while ((c1.isSynthetic() || isEJBProxy(c1)) && !Object.class.equals(c1))
      {
         c1 = c1.getSuperclass();
      }
      Object o2 = cv2.getLeafBean();
      Class<?> c2 = o2.getClass();
      while ((c2.isSynthetic() || isEJBProxy(c2)) && !Object.class.equals(c2))
      {
         c2 = c2.getSuperclass();
      }
      return c1.equals(c2);
   }

   private static boolean comparePropertyPath(Path p1, Path p2)
   {
      if (p1 == p2)
      {
         return true;
      }
      if (p1 == null || p2 == null)
      {
         return false;
      }
      if (p1.getClass() != p2.getClass())
      {
         return false;
      }
      Iterator<Node> it1 = p1.iterator();
      Iterator<Node> it2 = p2.iterator();
      while (true)
      {
         if (it1.hasNext() && it2.hasNext())
         {
            if (!compareNode(it1.next(), it2.next()))
            {
               return false;
            }
         }
         else
         {
            return it1.hasNext() == it2.hasNext();
         }
      }
   }

   private static boolean compareNode(Node n1, Node n2)
   {
      if (n1 == n2)
      {
         return true;
      }
      if (n1 == null || n2 == null)
      {
         return false;
      }
      if (n1.getClass() != n2.getClass())
      {
         return false;
      }
      if (!(n1 instanceof NodeImpl))
      {
         return false;
      }
      NodeImpl ni1 = (NodeImpl) n1;
      NodeImpl ni2 = (NodeImpl) n2;
      if (ni1.getIndex() == null)
      {
         if (ni2.getIndex() != null)
         {
            return false;
         }
      }
      else if (!ni1.getIndex().equals(ni2.getIndex()))
      {
         return false;
      }
      if (ni1.isInIterable() != ni2.isInIterable())
      {
         return false;
      }
      if (ni1.getKey() == null)
      {
         if (ni2.getKey() != null)
         {
            return false;
         }
      }
      else if (!ni1.getKey().equals(ni2.getKey()))
      {
         return false;
      }
      if (ni1.getKind() != ni2.getKind())
      {
         return false;
      }
      if (ni1.getName() == null) {
         if (ni2.getName() != null) {
            return false;
         }
      }
      else if (!ni1.getName().equals(ni2.getName()))
      {
         return false;
      }
      if (ni1.getKind().equals(ElementKind.PARAMETER) && ni2.getKind().equals(ElementKind.PARAMETER) && ni1.getParameterIndex() != ni2.getParameterIndex())
      {
         return false;
      }
      if (ni1.getParameterTypes() == null)
      {
         if (ni2.getParameterTypes() != null)
         {
            return false;
         }
      }
      else if (!compareClassList(ni1.getParameterTypes(), ni2.getParameterTypes()))
      {
         return false;
      }
      if (ni1.getParent() == null)
      {
         if (ni2.getParent() != null)
         {
            return false;
         }
      }
      else if (!compareNode(ni1.getParent(), ni2.getParent()))
      {
         return false;
      }
      return true;
   }

   private static boolean compareClassList(List<Class<?>> l1, List<Class<?>> l2)
   {
      if (l1.size() != l2.size())
      {
         return false;
      }
      for (int i = 0; i < l1.size(); i++)
      {
         if (l1.get(i) != l2.get(i))
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Determine whether an object is indeed a valid EJB proxy object created by this API.
    *
    * @param object the object to test
    * @return {@code true} if it is an EJB proxy, {@code false} otherwise
    */
   private static boolean isEJBProxy(final Class<?> clazz) {
      return clazz.getName().contains("$$$view");
   }
}
