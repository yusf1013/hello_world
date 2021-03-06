package physicsEngine.CollisionModule;

import mathHandler.VectorGeometry;
import threeDItems.Mesh;
import threeDItems.ObbMesh;
import threeDItems.Vec3d;

import java.util.ArrayList;
import java.util.Vector;

public class Collider {

    float xyOverlap=-1f, yzOverlap=-1f, xzOverlap=-1f, xy=0, yz=0, xz=0;

    public boolean detectCollision(Mesh m1, Mesh m2)
    {

        if(m1==null || m2==null)
        {
            return false;
        }

        if(m1.obb==null || m2.obb==null)
        {
            return false;
        }

        if(!collidesWith(m1.obb.cube, m2.obb.cube))
        {
            return false;
        }

        m1.updateObbList();
        m2.updateObbList();

        ArrayList<ObbMesh> al1 = new ArrayList<>(), al2 = new ArrayList<>();
        collidesWith(m1.obb.cube, m2.obbList, al2);
        collidesWith(m2.obb.cube, m1.obbList, al1);

        if(collidesWith_Static(al1, al2))
        {
            m1.collidedWith=m2;
            m2.collidedWith=m1;
            Vec3d c1=m1.obb.cube.getCenter(), c2=m2.obb.cube.getCenter(), d;
            VectorGeometry vg = new VectorGeometry();
            d=vg.vectorSub(c1, c2);

            //more ja
            float s = (float)Math.sqrt(vg.dotProduct(d, d)), overlap = Math.max(Math.max(xyOverlap, xzOverlap), yzOverlap);
            //float s = (float)Math.sqrt(vg.dotProduct(d, d)), overlap = Math.max(Math.max(xy, xz), yz);
            if((m2.getxTranslation()-m2.oxt)==0 && (m2.getyTranslation()-m2.oyt)==0 && (m2.getzTranslation()-m2.ozt)==0)
            {
                m2.setxTranslation(m2.getxTranslation() - overlap * d.x / s);
                m2.setyTranslation(m2.getyTranslation() - overlap * d.y / s);
                m2.setzTranslation(m2.getzTranslation() - overlap * d.z / s);
                //m2.setzTranslation(m2.ozt);
            }
            else
            {
                float a=1,b=1,c=1;
                if((m2.getxTranslation()==m2.oxt))
                    a=0;
                if((m2.getyTranslation()==m2.oyt))
                    b=0;
                if((m2.getzTranslation()==m2.ozt))
                    c=0;
                //m2.setxTranslation(m2.getxTranslation() - (overlap * d.x / s * a));
                //m2.setyTranslation(m2.getyTranslation() - (overlap * d.y / s * b));
                //m2.setzTranslation(m2.getzTranslation() - (overlap * d.z / s * c));
                m2.setyTranslation(m2.oyt);
                m2.setxTranslation(m2.oxt);
                m2.setzTranslation(m2.ozt);
            }
        }
        else
        {
            m2.collidedWith=null;
            m1.collidedWith=null;
        }
        return false;
    }

    public boolean collidesWith(ObbMesh m1, ArrayList<ObbMesh> al)
    {
        boolean flag=false;
        for(ObbMesh m: al)
            if(collidesWith(m, m1))
            {
                flag = true;
            }

        return flag;
    }

    public boolean collidesWith(ObbMesh m1, ArrayList<ObbMesh> al, ArrayList<ObbMesh> al2)
    {
        boolean flag=false;
        for(ObbMesh m: al)
            if(collidesWith(m, m1))
            {
                al2.add(m);
                flag = true;
            }

        return flag;
    }

    public boolean collidesWith(ArrayList<ObbMesh> al, ArrayList<ObbMesh> al2)
    {
        boolean flag=false;
        for(ObbMesh m: al)
            if(collidesWith(m, al2))
            {
                flag = true;
            }

        return flag;
    }

    public boolean collidesWith_Static(ArrayList<ObbMesh> al, ArrayList<ObbMesh> al2)
    {
        boolean flag=false;
        for(ObbMesh m: al)
            for(ObbMesh m2: al2)
                if(collidesWith_Static(m, m2))
                    flag=true;

        return flag;
    }

    public boolean collidesWith(ObbMesh m1, ObbMesh m2)
    {
        return (shapeOverlapSAT(m1, m2, "xy") && shapeOverlapSAT(m1, m2, "yz") && shapeOverlapSAT(m1, m2, "xz"));
    }

    public boolean collidesWith_Static(ObbMesh m1, ObbMesh m2)
    {
        float xy=xyOverlap, xz=xzOverlap, yz=yzOverlap, a=xy, b=yz, c=xz;
        if(shapeOverlapSAT_Static(m1, m2, "xy") && shapeOverlapSAT_Static(m1, m2, "yz") && shapeOverlapSAT_Static(m1, m2, "xz"))
        {
            return true;
        }
        else
        {
            xyOverlap=xy;
            xzOverlap=xz;
            yzOverlap=yz;
            xy=a;
            yz=b;
            xz=c;
            return false;
        }
    }

    public boolean shapeOverlapSAT(ObbMesh m1, ObbMesh m2, String axes)
    {
        ObbMesh poly1 = m1;
        ObbMesh poly2 = m2;

        poly1.p.clear();
        poly2.p.clear();

        if(!(axes.equals("xy") || axes.equals("xz") || axes.equals("yz")))
            throw new IllegalArgumentException("wrong axes data");

        Vector <Vec3d> vec = null;
        if(axes.equals("xy"))
            vec = poly1.vecXY;
        else if(axes.equals("xz"))
            vec = poly1.vecXZ;
        else if(axes.equals("yz"))
            vec = poly1.vecYZ;

        for(Vec3d v: vec)
        {
            poly1.p.add(VectorGeometry.multiplyMatrixAndVector(poly1.getWorldMat(), v));
            poly2.p.add(VectorGeometry.multiplyMatrixAndVector(poly2.getWorldMat(), v));
        }

        for (int shape = 0; shape < 2; shape++)
        {
            if (shape == 1)
            {
                poly1 = m2;
                poly2 = m1;
            }


            for (int a = 0; a < poly1.p.size(); a++)
            {
                int b = (a + 1) % poly1.p.size();
                Vec2d axisProj;
                if(axes.equals("xy"))
                    axisProj = new Vec2d( -(poly1.p.get(b).y - poly1.p.get(a).y), poly1.p.get(b).x - poly1.p.get(a).x );
                else if(axes.equals("xz"))
                    axisProj = new Vec2d( -(poly1.p.get(b).z - poly1.p.get(a).z), poly1.p.get(b).x - poly1.p.get(a).x );
                else
                    axisProj = new Vec2d( -(poly1.p.get(b).z - poly1.p.get(a).z), poly1.p.get(b).y - poly1.p.get(a).y );


                // Work out min and max 1D points for r1
                float min_r1 = 1000000, max_r1 = -1000000;
                for (int p = 0; p < poly1.p.size(); p++)
                {
                    float q;
                    if(axes.equals("xy"))
                        q = (poly1.p.get(p).x * axisProj.x + poly1.p.get(p).y * axisProj.y);
                    else if(axes.equals("xz"))
                        q = (poly1.p.get(p).x * axisProj.x + poly1.p.get(p).z * axisProj.y);
                    else
                        q = (poly1.p.get(p).y * axisProj.x + poly1.p.get(p).z * axisProj.y);

                    min_r1 = Math.min(min_r1, q);
                    max_r1 = Math.max(max_r1, q);
                }

                // Work out min and max 1D points for r2
                float min_r2 = 1000000, max_r2 = -1000000;
                for (int p = 0; p < poly2.p.size(); p++)
                {
                    float q;
                    if(axes.equals("xy"))
                        q = (poly2.p.get(p).x * axisProj.x + poly2.p.get(p).y * axisProj.y);
                    else if(axes.equals("xz"))
                        q = (poly2.p.get(p).x * axisProj.x + poly2.p.get(p).z * axisProj.y);
                    else
                        q = (poly2.p.get(p).y * axisProj.x + poly2.p.get(p).z * axisProj.y);

                    min_r2 = Math.min(min_r2, q);
                    max_r2 = Math.max(max_r2, q);
                }

                if (!(max_r2 >= min_r1 && max_r1 >= min_r2))
                {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean shapeOverlapSAT_Static(ObbMesh m1, ObbMesh m2, String axes)
    {
        ObbMesh poly1 = m1;
        ObbMesh poly2 = m2;
        float overlap = 1000000;

        poly1.p.clear();
        poly2.p.clear();

        if(!(axes.equals("xy") || axes.equals("xz") || axes.equals("yz")))
            throw new IllegalArgumentException("wrong axes data");

        Vector <Vec3d> vec = null;
        if(axes.equals("xy"))
            vec = poly1.vecXY;
        else if(axes.equals("xz"))
            vec = poly1.vecXZ;
        else if(axes.equals("yz"))
            vec = poly1.vecYZ;

        for(Vec3d v: vec)
        {
            poly1.p.add(VectorGeometry.multiplyMatrixAndVector(poly1.getWorldMat(), v));
            poly2.p.add(VectorGeometry.multiplyMatrixAndVector(poly2.getWorldMat(), v));
        }

        for (int shape = 0; shape < 2; shape++)
        {
            if (shape == 1)
            {
                poly1 = m2;
                poly2 = m1;
            }


            for (int a = 0; a < poly1.p.size(); a++)
            {
                int b = (a + 1) % poly1.p.size();
                Vec2d axisProj;
                if(axes.equals("xy"))
                    axisProj = new Vec2d( -(poly1.p.get(b).y - poly1.p.get(a).y), poly1.p.get(b).x - poly1.p.get(a).x );
                else if(axes.equals("xz"))
                    axisProj = new Vec2d( -(poly1.p.get(b).z - poly1.p.get(a).z), poly1.p.get(b).x - poly1.p.get(a).x );
                else
                    axisProj = new Vec2d( -(poly1.p.get(b).z - poly1.p.get(a).z), poly1.p.get(b).y - poly1.p.get(a).y );


                // Work out min and max 1D points for r1
                float min_r1 = 1000000, max_r1 = -1000000;
                for (int p = 0; p < poly1.p.size(); p++)
                {
                    float q;
                    if(axes.equals("xy"))
                        q = (poly1.p.get(p).x * axisProj.x + poly1.p.get(p).y * axisProj.y);
                    else if(axes.equals("xz"))
                        q = (poly1.p.get(p).x * axisProj.x + poly1.p.get(p).z * axisProj.y);
                    else
                        q = (poly1.p.get(p).y * axisProj.x + poly1.p.get(p).z * axisProj.y);

                    min_r1 = Math.min(min_r1, q);
                    max_r1 = Math.max(max_r1, q);
                }

                // Work out min and max 1D points for r2
                float min_r2 = 1000000, max_r2 = -1000000;
                for (int p = 0; p < poly2.p.size(); p++)
                {
                    float q;
                    if(axes.equals("xy"))
                        q = (poly2.p.get(p).x * axisProj.x + poly2.p.get(p).y * axisProj.y);
                    else if(axes.equals("xz"))
                        q = (poly2.p.get(p).x * axisProj.x + poly2.p.get(p).z * axisProj.y);
                    else
                        q = (poly2.p.get(p).y * axisProj.x + poly2.p.get(p).z * axisProj.y);

                    min_r2 = Math.min(min_r2, q);
                    max_r2 = Math.max(max_r2, q);
                }

                //System.out.println("overlap before: " + overlap);
                overlap = Math.min(Math.min(max_r1, max_r2) - Math.max(min_r1, min_r2), overlap);
                //System.out.println(min_r1 + ", " + max_r1 + ", " + min_r2 + ", " + max_r2);
                //System.out.println("calculated overlap: " + overlap);

                if (!(max_r2 >= min_r1 && max_r1 >= min_r2))
                {
                    //System.out.println("overlap zero and return false");
                    return false;
                }
            }
        }

        if(axes.equals("xy") && xyOverlap<overlap)
            xyOverlap=overlap;
        else if(axes.equals("yz") && yzOverlap<overlap)
            yzOverlap=overlap;
        else if(axes.equals("xz") && xzOverlap<overlap)
            xzOverlap=overlap;

        if(axes.equals("xy") && xyOverlap<overlap)
            xy+=overlap;
        else if(axes.equals("yz") && yzOverlap<overlap)
            yz+=overlap;
        else if(axes.equals("xz") && xzOverlap<overlap)
            xz+=overlap;

        //System.out.println("legit overlap and return true");
        return true;
    }

}

class Vec2d {
    float x,y;
    public Vec2d(float x, float y)
    {
        this.x=x;
        this.y=y;
    }
}
