package cats
package free

import cats._
import cats.data._
import cats.laws.discipline._
import cats.tests.{CatsSuite, ListWrapper}

import org.scalacheck.{Arbitrary, Gen}

class FreeTTests extends CatsSuite {

  import ListWrapper._
  import FreeTTests._

  {
    implicit val listWrapperApplicative: Applicative[ListWrapper] = ListWrapper.applicative
    implicit val listWrapperFlatMap: FlatMap[ListWrapper] = ListWrapper.monadCombine
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListWrapper]
    implicit val catsFlatMapForFreeT = FreeT.catsFreeFlatMapForFreeT[ListWrapper, ListWrapper]
    checkAll("FreeT[ListWrapper, ListWrapper, Int]", FlatMapTests[FreeTListWrapper].flatMap[Int, Int, Int])
    checkAll("FlatMap[FreeT[ListWrapper, ListWrapper, ?]]", SerializableTests.serializable(FlatMap[FreeTListWrapper]))
  }

  {
    implicit val listWrapperApplicative: Applicative[ListWrapper] = ListWrapper.applicative
    implicit val listWrapperFlatMapRec: FlatMapRec[ListWrapper] = ListWrapper.flatMapRec
    implicit val catsFlatMapRecForFreeT = FreeT.catsFreeMonadForFreeT[ListWrapper, ListWrapper]
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListWrapper]
    checkAll("FreeT[ListWrapper, ListWrapper, Int]", FlatMapRecTests[FreeTListWrapper].flatMapRec[Int, Int, Int])
    checkAll("FlatMapRec[FreeT[ListWrapper, ListWrapper, ?]]", SerializableTests.serializable(FlatMapRec[FreeTListWrapper]))
  }

  {
    implicit val listWrapperApplicative: Applicative[ListWrapper] = ListWrapper.applicative
    implicit val catsMonadForFreeT = FreeT.catsFreeMonadForFreeT[ListWrapper, ListWrapper]
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListWrapper]
    checkAll("FreeT[ListWrapper, ListWrapper, Int]", MonadTests[FreeTListWrapper].monad[Int, Int, Int])
    checkAll("Monad[FreeT[ListWrapper, ListWrapper, ?]]", SerializableTests.serializable(Monad[FreeTListWrapper]))
  }

  {
    implicit val listWrapperApplicative: Applicative[ListWrapper] = ListWrapper.applicative
    implicit val listWrapperFlatMapRec: FlatMapRec[ListWrapper] = ListWrapper.flatMapRec
    implicit val listSemigroupK: SemigroupK[ListWrapper] = ListWrapper.semigroupK
    implicit val catsMonadCombineForFreeT = FreeT.catsFreeCombineForFreeT[ListWrapper, ListWrapper]
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListWrapper]
    checkAll("FreeT[ListWrapper, ListWrapper, Int]", SemigroupKTests[FreeTListWrapper].semigroupK[Int])
    checkAll("SemigroupK[FreeT[ListWrapper, ListWrapper, ?]]", SerializableTests.serializable(SemigroupK[FreeTListWrapper]))
  }

  {
    implicit val listWrapperApplicative: Alternative[ListWrapper] = ListWrapper.alternative
    implicit val listWrapperFlatMapRec: FlatMapRec[ListWrapper] = ListWrapper.flatMapRec
    implicit val catsMonadCombineForFreeT = FreeT.catsFreeMonadCombineForFreeT[ListWrapper, ListWrapper]
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListWrapper]
    checkAll("FreeT[ListWrapper, ListWrapper, Int]", MonadCombineTests[FreeTListWrapper].monadCombine[Int, Int, Int])
    checkAll("MonadCombine[FreeT[ListWrapper, ListWrapper, ?]]", SerializableTests.serializable(MonadCombine[FreeTListWrapper]))
  }

  {
    import cats.data.XorT
    implicit val listWrapperFlatMapRec: FlatMapRec[ListWrapper] = ListWrapper.flatMapRec
    implicit val catsMonadErrorForFreeT = FreeT.catsFreeMonadErrorForFreeT[ListWrapper, Option, Unit]
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListOption]
    implicit val eqXortTFA: Eq[XorT[FreeTListOption, Unit, Int]] = XorT.catsDataEqForXorT[FreeTListOption, Unit, Int]
    checkAll("FreeT[ListWrapper, Option, Int]", MonadErrorTests[FreeTListOption, Unit].monadError[Int, Int, Int])
    checkAll("MonadError[FreeT[ListWrapper, Option, ?]]", SerializableTests.serializable(MonadError[FreeTListOption, Unit]))
  }
  
  {
    implicit val iso = CartesianTests.Isomorphisms.invariant[FreeTListState]
    implicit val catsMonadStateForFreeT = FreeT.catsFreeMonadStateForFreeT[IntState, IntState, Int]
    checkAll("FreeT[ListWrapper, State[Int, ?], Int]", MonadStateTests[FreeTListState, Int].monadState[Int, Int, Int])
    checkAll("MonadState[FreeT[ListWrapper,State[Int, ?], ?], Int]", SerializableTests.serializable(MonadState[FreeTListState, Int]))
  }

}

object FreeTTests extends FreeTTestsInstances

sealed trait FreeTTestsInstances {

  import Arbitrary._
  import org.scalacheck.Arbitrary
  import cats.kernel.instances.option._
  import cats.tests.StateTTests._

  type IntState[A] = State[Int, A]
  type FreeTListW[M[_], A] = FreeT[ListWrapper, M, A]
  type FreeTListWrapper[A] = FreeTListW[ListWrapper, A]
  type FreeTListOption[A] = FreeTListW[Option, A]
  type FreeTListState[A] = FreeT[IntState, IntState, A]

  implicit val intEq : Eq[Int] = new Eq[Int] {
    def eqv(a : Int, b : Int) = a == b
  }

  implicit def intStateEq[A : Eq] : Eq[IntState[A]] = stateEq[Int, A]

  implicit def evalEq[A : Eq] : Eq[Eval[A]] = Eval.catsEqForEval[A]

  implicit def listWrapperArbitrary[A: Arbitrary]: Arbitrary[ListWrapper[A]] = ListWrapper.listWrapperArbitrary[A]
    
  implicit def freeTStateArb[A : Arbitrary](implicit LA : Arbitrary[ListWrapper[A]]) : Arbitrary[FreeTListState[A]] = freeTArb[IntState, IntState, A]
    
  implicit def freeTArb[F[_], G[_]: Applicative, A](implicit F: Arbitrary[F[A]], G: Arbitrary[G[A]], A: Arbitrary[A]): Arbitrary[FreeT[F, G, A]] =
    Arbitrary(freeTGen[F, G, A](4))


  implicit def freeTListWrapperEq[A](implicit A: Eq[A]): Eq[FreeTListWrapper[A]] = new Eq[FreeTListWrapper[A]] {
    implicit val listWrapperMonad: MonadRec[ListWrapper] = ListWrapper.monadRec
    def eqv(a: FreeTListWrapper[A], b: FreeTListWrapper[A]) = Eq[ListWrapper[A]].eqv(a.runM(identity), b.runM(identity))
  }

  implicit def freeTListOptionEq[A](implicit A: Eq[A], OF: MonadRec[Option]): Eq[FreeTListOption[A]] = new Eq[FreeTListOption[A]] {
    implicit val listWrapperMonad: MonadRec[ListWrapper] = ListWrapper.monadRec
    def eqv(a: FreeTListOption[A], b: FreeTListOption[A]) = Eq[Option[A]].eqv(a.runM(_.list.headOption), b.runM(_.list.headOption))
  }

  implicit def freeTListStateEq[A](implicit A : Eq[A], SM: MonadRec[IntState]): Eq[FreeTListState[A]] = new Eq[FreeTListState[A]] {
    def eqv(a: FreeTListState[A], b: FreeTListState[A]) = Eq[State[Int, A]].eqv(a.runM(identity), b.runM(identity))
  }

  private def freeTGen[F[_], G[_]: Applicative, A](maxDepth: Int)(implicit F: Arbitrary[F[A]], G: Arbitrary[G[A]], A: Arbitrary[A]): Gen[FreeT[F, G, A]] = {
    val noFlatMapped = Gen.oneOf(
      A.arbitrary.map(FreeT.pure[F, G, A]),
      F.arbitrary.map(FreeT.liftF[F, G, A])
    )

    val nextDepth = Gen.chooseNum(1, maxDepth - 1)

    def withFlatMapped = for {
      fDepth <- nextDepth
      freeDepth <- nextDepth
      f <- arbFunction1[A, FreeT[F, G, A]](Arbitrary(freeTGen[F, G, A](fDepth))).arbitrary
      freeFGA <- freeTGen[F, G, A](freeDepth)
    } yield freeFGA.flatMap(f)

    if (maxDepth <= 1) noFlatMapped
    else Gen.oneOf(noFlatMapped, withFlatMapped)
  }

}
