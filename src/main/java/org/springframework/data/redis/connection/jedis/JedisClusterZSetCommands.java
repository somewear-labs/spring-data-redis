/*
 * Copyright 2017-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection.jedis;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ZParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.ClusterSlotHashUtil;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.convert.SetConverter;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanCursor;
import org.springframework.data.redis.core.ScanIteration;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.util.ByteUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Clement Ong
 * @author Andrey Shlykov
 * @author Jens Deppe
 * @since 2.0
 */
class JedisClusterZSetCommands implements RedisZSetCommands {

	private static final SetConverter<redis.clients.jedis.Tuple, Tuple> TUPLE_SET_CONVERTER = new SetConverter<>(
			JedisConverters::toTuple);
	private final JedisClusterConnection connection;

	JedisClusterZSetCommands(JedisClusterConnection connection) {
		this.connection = connection;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zAdd(byte[], double, byte[])
	 */
	@Override
	public Boolean zAdd(byte[] key, double score, byte[] value, ZAddArgs args) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		try {
			return JedisConverters
					.toBoolean(connection.getCluster().zadd(key, score, value, JedisConverters.toZAddParams(args)));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zAdd(byte[], java.util.Set, org.springframework.data.redis.connection.RedisZSetCommands.ZAddArgs)
	 */
	@Override
	public Long zAdd(byte[] key, Set<Tuple> tuples, ZAddArgs args) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(tuples, "Tuples must not be null!");

		try {
			return connection.getCluster().zadd(key, JedisConverters.toTupleMap(tuples), JedisConverters.toZAddParams(args));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRem(byte[], byte[][])
	 */
	@Override
	public Long zRem(byte[] key, byte[]... values) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(values, "Values must not be null!");
		Assert.noNullElements(values, "Values must not contain null elements!");

		try {
			return connection.getCluster().zrem(key, values);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zIncrBy(byte[], double, byte[])
	 */
	@Override
	public Double zIncrBy(byte[] key, double increment, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		try {
			return connection.getCluster().zincrby(key, increment, value);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRandMember(byte[])
	 */
	@Override
	public byte[] zRandMember(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zrandmember(key);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRandMember(byte[], long)
	 */
	@Override
	public List<byte[]> zRandMember(byte[] key, long count) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return new ArrayList<>(connection.getCluster().zrandmember(key, count));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRandMemberWithScore(byte[])
	 */
	@Override
	public Tuple zRandMemberWithScore(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		try {
			Set<redis.clients.jedis.Tuple> tuples = connection.getCluster().zrandmemberWithScores(key, 1);

			return tuples.isEmpty() ? null : JedisConverters.toTuple(tuples.iterator().next());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRandMemberWithScore(byte[], long)
	 */
	@Override
	public List<Tuple> zRandMemberWithScore(byte[] key, long count) {

		Assert.notNull(key, "Key must not be null!");

		try {
			Set<redis.clients.jedis.Tuple> tuples = connection.getCluster().zrandmemberWithScores(key, count);

			return tuples.stream().map(JedisConverters::toTuple).collect(Collectors.toList());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRank(byte[], byte[])
	 */
	@Override
	public Long zRank(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		try {
			return connection.getCluster().zrank(key, value);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRank(byte[], byte[])
	 */
	@Override
	public Long zRevRank(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		try {
			return connection.getCluster().zrevrank(key, value);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRange(byte[], long, long)
	 */
	@Override
	public Set<byte[]> zRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zrange(key, start, end);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScoreWithScores(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range cannot be null for ZRANGEBYSCOREWITHSCORES.");

		byte[] min = JedisConverters.boundaryToBytesForZRange(range.getMin(), JedisConverters.NEGATIVE_INFINITY_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRange(range.getMax(), JedisConverters.POSITIVE_INFINITY_BYTES);

		try {
			if (limit.isUnlimited()) {
				return toTupleSet(connection.getCluster().zrangeByScoreWithScores(key, min, max));
			}
			return toTupleSet(
					connection.getCluster().zrangeByScoreWithScores(key, min, max, limit.getOffset(), limit.getCount()));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRevRangeByScore(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range cannot be null for ZREVRANGEBYSCORE.");

		byte[] min = JedisConverters.boundaryToBytesForZRange(range.getMin(), JedisConverters.NEGATIVE_INFINITY_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRange(range.getMax(), JedisConverters.POSITIVE_INFINITY_BYTES);

		try {
			if (limit.isUnlimited()) {
				return connection.getCluster().zrevrangeByScore(key, max, min);
			}
			return connection.getCluster().zrevrangeByScore(key, max, min, limit.getOffset(), limit.getCount());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScoreWithScores(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range cannot be null for ZREVRANGEBYSCOREWITHSCORES.");

		byte[] min = JedisConverters.boundaryToBytesForZRange(range.getMin(), JedisConverters.NEGATIVE_INFINITY_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRange(range.getMax(), JedisConverters.POSITIVE_INFINITY_BYTES);

		try {
			if (limit.isUnlimited()) {
				return toTupleSet(connection.getCluster().zrevrangeByScoreWithScores(key, max, min));
			}
			return toTupleSet(
					connection.getCluster().zrevrangeByScoreWithScores(key, max, min, limit.getOffset(), limit.getCount()));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zCount(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zCount(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range cannot be null for ZCOUNT.");

		byte[] min = JedisConverters.boundaryToBytesForZRange(range.getMin(), JedisConverters.NEGATIVE_INFINITY_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRange(range.getMax(), JedisConverters.POSITIVE_INFINITY_BYTES);

		try {
			return connection.getCluster().zcount(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zLexCount(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zLexCount(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range must not be null!");

		byte[] min = JedisConverters.boundaryToBytesForZRangeByLex(range.getMin(), JedisConverters.MINUS_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRangeByLex(range.getMax(), JedisConverters.PLUS_BYTES);

		try {
			return connection.getCluster().zlexcount(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zPopMin(byte[])
	 */
	@Nullable
	@Override
	public Tuple zPopMin(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		try {
			redis.clients.jedis.Tuple tuple = connection.getCluster().zpopmin(key);
			return tuple != null ? JedisConverters.toTuple(tuple) : null;
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zPopMin(byte[], long)
	 */
	@Nullable
	@Override
	public Set<Tuple> zPopMin(byte[] key, long count) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return toTupleSet(connection.getCluster().zpopmin(key, Math.toIntExact(count)));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#bZPopMin(byte[], long, java.util.concurrent.TimeUnit)
	 */
	@Nullable
	@Override
	public Tuple bZPopMin(byte[] key, long timeout, TimeUnit unit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(unit, "TimeUnit must not be null!");

		try {
			return toTuple(connection.getCluster().bzpopmin(JedisConverters.toSeconds(timeout, unit), key));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zPopMax(byte[])
	 */
	@Nullable
	@Override
	public Tuple zPopMax(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		try {
			redis.clients.jedis.Tuple tuple = connection.getCluster().zpopmax(key);
			return tuple != null ? JedisConverters.toTuple(tuple) : null;
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zPopMax(byte[], long)
	 */
	@Nullable
	@Override
	public Set<Tuple> zPopMax(byte[] key, long count) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return toTupleSet(connection.getCluster().zpopmax(key, Math.toIntExact(count)));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#bZPopMax(byte[], long, java.util.concurrent.TimeUnit)
	 */
	@Nullable
	@Override
	public Tuple bZPopMax(byte[] key, long timeout, TimeUnit unit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(unit, "TimeUnit must not be null!");

		try {
			return toTuple(connection.getCluster().bzpopmax(JedisConverters.toSeconds(timeout, unit), key));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRangeByScore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zRemRangeByScore(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range cannot be null for ZREMRANGEBYSCORE.");

		byte[] min = JedisConverters.boundaryToBytesForZRange(range.getMin(), JedisConverters.NEGATIVE_INFINITY_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRange(range.getMax(), JedisConverters.POSITIVE_INFINITY_BYTES);

		try {
			return connection.getCluster().zremrangeByScore(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range cannot be null for ZRANGEBYSCORE.");

		byte[] min = JedisConverters.boundaryToBytesForZRange(range.getMin(), JedisConverters.NEGATIVE_INFINITY_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRange(range.getMax(), JedisConverters.POSITIVE_INFINITY_BYTES);

		try {
			if (limit.isUnlimited()) {
				return connection.getCluster().zrangeByScore(key, min, max);
			}
			return connection.getCluster().zrangeByScore(key, min, max, limit.getOffset(), limit.getCount());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByLex(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRangeByLex(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range must not be null for ZRANGEBYLEX!");
		Assert.notNull(limit, "Limit must not be null!");

		byte[] min = JedisConverters.boundaryToBytesForZRangeByLex(range.getMin(), JedisConverters.MINUS_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRangeByLex(range.getMax(), JedisConverters.PLUS_BYTES);

		try {
			if (limit.isUnlimited()) {
				return connection.getCluster().zrangeByLex(key, min, max);
			}
			return connection.getCluster().zrangeByLex(key, min, max, limit.getOffset(), limit.getCount());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRangeByLex(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zRemRangeByLex(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range must not be null for ZREMRANGEBYLEX!");

		byte[] min = JedisConverters.boundaryToBytesForZRangeByLex(range.getMin(), JedisConverters.MINUS_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRangeByLex(range.getMax(), JedisConverters.PLUS_BYTES);

		try {
			return connection.getCluster().zremrangeByLex(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByLex(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRevRangeByLex(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range must not be null for ZREVRANGEBYLEX!");
		Assert.notNull(limit, "Limit must not be null!");

		byte[] min = JedisConverters.boundaryToBytesForZRangeByLex(range.getMin(), JedisConverters.MINUS_BYTES);
		byte[] max = JedisConverters.boundaryToBytesForZRangeByLex(range.getMax(), JedisConverters.PLUS_BYTES);

		try {
			if (limit.isUnlimited()) {
				return connection.getCluster().zrevrangeByLex(key, max, min);
			}
			return connection.getCluster().zrevrangeByLex(key, max, min, limit.getOffset(), limit.getCount());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeWithScores(byte[], long, long)
	 */
	@Override
	public Set<Tuple> zRangeWithScores(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return toTupleSet(connection.getCluster().zrangeWithScores(key, start, end));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], double, double)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, double min, double max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zrangeByScore(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScoreWithScores(byte[], double, double)
	 */
	@Override
	public Set<Tuple> zRangeByScoreWithScores(byte[] key, double min, double max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return toTupleSet(connection.getCluster().zrangeByScoreWithScores(key, min, max));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], double, double, long, long)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, double min, double max, long offset, long count) {

		Assert.notNull(key, "Key must not be null!");

		if (offset > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Count/Offset cannot exceed Integer.MAX_VALUE!");
		}

		try {
			return connection.getCluster().zrangeByScore(key, min, max, Long.valueOf(offset).intValue(),
					Long.valueOf(count).intValue());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScoreWithScores(byte[], double, double, long, long)
	 */
	@Override
	public Set<Tuple> zRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {

		Assert.notNull(key, "Key must not be null!");

		if (offset > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Count/Offset cannot exceed Integer.MAX_VALUE!");
		}

		try {
			return toTupleSet(connection.getCluster().zrangeByScoreWithScores(key, min, max, Long.valueOf(offset).intValue(),
					Long.valueOf(count).intValue()));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRange(byte[], long, long)
	 */
	@Override
	public Set<byte[]> zRevRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zrevrange(key, start, end);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeWithScores(byte[], long, long)
	 */
	@Override
	public Set<Tuple> zRevRangeWithScores(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return toTupleSet(connection.getCluster().zrevrangeWithScores(key, start, end));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScore(byte[], double, double)
	 */
	@Override
	public Set<byte[]> zRevRangeByScore(byte[] key, double min, double max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zrevrangeByScore(key, max, min);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScoreWithScores(byte[], double, double)
	 */
	@Override
	public Set<Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return toTupleSet(connection.getCluster().zrevrangeByScoreWithScores(key, max, min));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScore(byte[], double, double, long, long)
	 */
	@Override
	public Set<byte[]> zRevRangeByScore(byte[] key, double min, double max, long offset, long count) {

		Assert.notNull(key, "Key must not be null!");

		if (offset > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Count/Offset cannot exceed Integer.MAX_VALUE!");
		}

		try {
			return connection.getCluster().zrevrangeByScore(key, max, min, Long.valueOf(offset).intValue(),
					Long.valueOf(count).intValue());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScoreWithScores(byte[], double, double, long, long)
	 */
	@Override
	public Set<Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {

		Assert.notNull(key, "Key must not be null!");

		if (offset > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Count/Offset cannot exceed Integer.MAX_VALUE!");
		}

		try {
			return toTupleSet(connection.getCluster().zrevrangeByScoreWithScores(key, max, min,
					Long.valueOf(offset).intValue(), Long.valueOf(count).intValue()));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zCount(byte[], double, double)
	 */
	@Override
	public Long zCount(byte[] key, double min, double max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zcount(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zCard(byte[])
	 */
	@Override
	public Long zCard(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zcard(key);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zScore(byte[], byte[])
	 */
	@Override
	public Double zScore(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		try {
			return connection.getCluster().zscore(key, value);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zMScore(byte[], byte[][])
	 */
	@Override
	public List<Double> zMScore(byte[] key, byte[][] values) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(values, "Values must not be null!");

		try {
			return connection.getCluster().zmscore(key, values);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRange(byte[], long, long)
	 */
	@Override
	public Long zRemRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zremrangeByRank(key, start, end);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRangeByScore(byte[], double, double)
	 */
	@Override
	public Long zRemRangeByScore(byte[] key, double min, double max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zremrangeByScore(key, min, max);
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zDiff(byte[][])
	 */
	@Override
	public Set<byte[]> zDiff(byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return connection.getCluster().zdiff(sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZDIFF can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zDiffWithScores(byte[][])
	 */
	@Override
	public Set<Tuple> zDiffWithScores(byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return JedisConverters.toTupleSet(connection.getCluster().zdiffWithScores(sets));
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZDIFF can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zDiffStore(byte[], byte[][])
	 */
	@Override
	public Long zDiffStore(byte[] destKey, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");

		byte[][] allKeys = ByteUtils.mergeArrays(destKey, sets);

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(allKeys)) {

			try {
				return connection.getCluster().zdiffStore(destKey, sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZDIFFSTORE can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInter(byte[][])
	 */
	@Override
	public Set<byte[]> zInter(byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return connection.getCluster().zinter(new ZParams(), sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZINTER can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterWithScores(byte[][])
	 */
	@Override
	public Set<Tuple> zInterWithScores(byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return JedisConverters.toTupleSet(connection.getCluster().zinterWithScores(new ZParams(), sets));
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZINTER can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterWithScores(org.springframework.data.redis.connection.RedisZSetCommands.Aggregate, org.springframework.data.redis.connection.RedisZSetCommands.Weights, byte[][])
	 */
	@Override
	public Set<Tuple> zInterWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");
		Assert.isTrue(weights.size() == sets.length, () -> String
				.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length));

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return JedisConverters
						.toTupleSet(connection.getCluster().zinterWithScores(toZParams(aggregate, weights), sets));
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZINTER can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], byte[][])
	 */
	@Override
	public Long zInterStore(byte[] destKey, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");

		byte[][] allKeys = ByteUtils.mergeArrays(destKey, sets);

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(allKeys)) {

			try {
				return connection.getCluster().zinterstore(destKey, sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZINTERSTORE can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Aggregate, org.springframework.data.redis.connection.RedisZSetCommands.Weights, byte[][])
	 */
	@Override
	public Long zInterStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");
		Assert.isTrue(weights.size() == sets.length, () -> String
				.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length));

		byte[][] allKeys = ByteUtils.mergeArrays(destKey, sets);

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(allKeys)) {

			try {
				return connection.getCluster().zinterstore(destKey, toZParams(aggregate, weights), sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new IllegalArgumentException("ZINTERSTORE can only be executed when all keys map to the same slot");
	}

	/*
	* (non-Javadoc)
	* @see org.springframework.data.redis.connection.RedisZSetCommands#zUnion(byte[][])
	*/
	@Override
	public Set<byte[]> zUnion(byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return connection.getCluster().zunion(new ZParams(), sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZUNION can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionWithScores(byte[][])
	 */
	@Override
	public Set<Tuple> zUnionWithScores(byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return JedisConverters.toTupleSet(connection.getCluster().zunionWithScores(new ZParams(), sets));
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZUNION can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionWithScores(org.springframework.data.redis.connection.RedisZSetCommands.Aggregate, org.springframework.data.redis.connection.RedisZSetCommands.Weights, byte[][])
	 */
	@Override
	public Set<Tuple> zUnionWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {

		Assert.notNull(sets, "Sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");
		Assert.isTrue(weights.size() == sets.length, () -> String
				.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length));

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(sets)) {

			try {
				return JedisConverters
						.toTupleSet(connection.getCluster().zunionWithScores(toZParams(aggregate, weights), sets));
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZUNION can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], byte[][])
	 */
	@Override
	public Long zUnionStore(byte[] destKey, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");

		byte[][] allKeys = ByteUtils.mergeArrays(destKey, sets);

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(allKeys)) {

			try {
				return connection.getCluster().zunionstore(destKey, sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZUNIONSTORE can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Aggregate, org.springframework.data.redis.connection.RedisZSetCommands.Weights, byte[][])
	 */
	@Override
	public Long zUnionStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");
		Assert.isTrue(weights.size() == sets.length, () -> String
				.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length));

		byte[][] allKeys = ByteUtils.mergeArrays(destKey, sets);

		if (ClusterSlotHashUtil.isSameSlotForAllKeys(allKeys)) {

			ZParams zparams = toZParams(aggregate, weights);

			try {
				return connection.getCluster().zunionstore(destKey, zparams, sets);
			} catch (Exception ex) {
				throw convertJedisAccessException(ex);
			}
		}

		throw new InvalidDataAccessApiUsageException("ZUNIONSTORE can only be executed when all keys map to the same slot");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zScan(byte[], org.springframework.data.redis.core.ScanOptions)
	 */
	@Override
	public Cursor<Tuple> zScan(byte[] key, ScanOptions options) {

		Assert.notNull(key, "Key must not be null!");

		return new ScanCursor<Tuple>(options) {

			@Override
			protected ScanIteration<Tuple> doScan(long cursorId, ScanOptions options) {

				ScanParams params = JedisConverters.toScanParams(options);

				redis.clients.jedis.ScanResult<redis.clients.jedis.Tuple> result = connection.getCluster().zscan(key,
						JedisConverters.toBytes(cursorId), params);
				return new ScanIteration<>(Long.valueOf(result.getCursor()),
						JedisConverters.tuplesToTuples().convert(result.getResult()));
			}
		}.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], java.lang.String, java.lang.String)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, String min, String max) {

		Assert.notNull(key, "Key must not be null!");

		try {
			return connection.getCluster().zrangeByScore(key, JedisConverters.toBytes(min), JedisConverters.toBytes(max));
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {

		Assert.notNull(key, "Key must not be null!");

		if (offset > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Count/Offset cannot exceed Integer.MAX_VALUE!");
		}

		try {
			return connection.getCluster().zrangeByScore(key, JedisConverters.toBytes(min), JedisConverters.toBytes(max),
					Long.valueOf(offset).intValue(), Long.valueOf(count).intValue());
		} catch (Exception ex) {
			throw convertJedisAccessException(ex);
		}
	}

	private DataAccessException convertJedisAccessException(Exception ex) {
		return connection.convertJedisAccessException(ex);
	}

	private static Set<Tuple> toTupleSet(Set<redis.clients.jedis.Tuple> source) {
		return TUPLE_SET_CONVERTER.convert(source);
	}

	private static ZParams toZParams(Aggregate aggregate, Weights weights) {
		return new ZParams().weights(weights.toArray()).aggregate(ZParams.Aggregate.valueOf(aggregate.name()));
	}

	/**
	 * Workaround for broken Jedis BZPOP signature.
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	private static Tuple toTuple(@Nullable List<?> bytes) {

		if (bytes == null || bytes.isEmpty()) {
			return null;
		}

		return new DefaultTuple((byte[]) bytes.get(1), Double.parseDouble(new String((byte[]) bytes.get(2))));
	}

}
