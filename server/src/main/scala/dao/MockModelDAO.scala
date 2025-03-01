package dao

import model.SampleModelObject
import zio.{ IO, ZIO }
import zioslick.RepositoryException

trait MockModelDAO extends ModelDAO {
  class MockDAO extends ModelDAO.Service {
    override def count: IO[RepositoryException, Int] = ZIO.succeed(10)

    override def report: IO[RepositoryException, String] = ZIO.succeed("This is a report")

    override def sampleModelObjects(): IO[RepositoryException, Seq[SampleModelObject]] = ZIO.succeed(Seq.empty)

    override def sampleModelObject(id: Int): IO[RepositoryException, Option[SampleModelObject]] = ZIO.succeed(None)

    override def upsert(obj: SampleModelObject): IO[RepositoryException, SampleModelObject] = ZIO.succeed(obj)

    override def delete(obj: SampleModelObject): IO[RepositoryException, Boolean] = ZIO.succeed(false)
  }

  override def modelDAO: ModelDAO.Service = new MockDAO
}
