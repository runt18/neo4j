/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v3_2.pipes

import org.neo4j.cypher.internal.compiler.v3_2.ExecutionContext
import org.neo4j.cypher.internal.compiler.v3_2.commands.expressions.Expression
import org.neo4j.cypher.internal.compiler.v3_2.helpers.ListSupport
import org.neo4j.cypher.internal.compiler.v3_2.planDescription.Id

case class ForeachPipe(source: Pipe, inner: Pipe, variable: String, expression: Expression)
                      (val id: Id = new Id)
                      (implicit pipeMonitor: PipeMonitor)
  extends PipeWithSource(source, pipeMonitor) with ListSupport {

  override protected def internalCreateResults(input: Iterator[ExecutionContext], state: QueryState): Iterator[ExecutionContext] =
    input.map {
      (outerContext) =>
        val values = makeTraversable(expression(outerContext)(state))
        values.foreach { v =>
          val innerState = state.withInitialContext(outerContext.newWith1(variable, v))
          inner.createResults(innerState).length // exhaust the iterator, in case there's a merge read increasing cardinality inside the foreach
        }
        outerContext
    }
}
