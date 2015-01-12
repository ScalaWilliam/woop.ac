<?php
$acp = array('i','i','mode'=>'i','curplayers'=>'i','remain'=>'i','map'=>'s','description'=>'s','maxplayers'=>'i','i');
//$acp = array('i','i','i','mode'=>'i','curplayers'=>'i','remain'=>'i','map'=>'s','description'=>'s');
$cppl = array('cn'=>'i','ping'=>'i','name'=>'s','team'=>'s','frags'=>'i',
'flagscore'=>'i','deaths'=>'i','teamkills'=>'i','accuracy'=>'i','health'=>'i',
'armour'=>'i','gunselect'=>'i', 'role'=>'i','state'=>'i');
class Server
{
	public $host = null;
	public $port = null;
	public $ip = null;
	public function __construct($data)
	{
		if ( isset($data['host']) )
		{
			$h = gethostbyname($data['host']);
			/*if ( $data['host'] == $h )
			{
				$this->host = $data['host'];
			}
			else
			{
				$this->host = $data['host'];
			}*/
			$this->host = $data['host'];
			$this->ip = $h;
		}
		if ( !isset($data['port']) )
		{
			$this->port = '28763';
		}
		else
		{
			$this->port = $data['port'];
		}
		$this->sock = stream_socket_client('udp://'.$this->ip.':'.($this->port + 1).'',$e);
		stream_set_timeout($this->sock, 0, 500);
		if ( !$this->sock )
		{
			throw new Exception("Could not create, error: '".$e."'");
			return false;
		}
	}
	public function ping()
	{
		if (@fwrite($this->sock,chr(19).chr(1)) === 0 )
		{
			throw new Exception("Ping socket part #1 failed to send.");
			return false;
		}
		if (@fwrite($this->sock,chr(0).chr(1).chr(255)) === 0 )
		{
			throw new Exception("Ping socket part #2 failed to send.");
			return false;
		}
		return true;
	}
	public function getbuf($ex = true)
	{
		$w = $e = array();
		$r = array($this->sock);
		if ( stream_select($r,$w,$e, 0, 10) === 0 )
		{
			if ( $ex ) { throw new Exception("No packet received..."); }
			return false;
		}
		$s = stream_socket_recvfrom($this->sock, 255);
		if ( $s === false )
		{
			if ( $ex ) { throw new Exception("No data from packet received..."); }
			return false;
		}
		return new Buffer($s);
	}
	public function sinfo($buf)
	{
		global $acp;
		$this->sinfo = $buf->fetch($acp);
		return true;
	}
	public function pinfo($buf)
	{
		global $cppl;
		for ( $i =0; $i < 6; $i++ ) { getint($buf); }
		//var_dump($buf);
		$players = 0;
		$p = array();
		while ( ($int = getint($buf)) !== NULL ) { $players++; }
		for ( $i =0; $i < $players; $i++ )
		{
			for ( $i = 0; $i < 6; $i++ )
			{
				var_dump(getint($buf)); 
			}
			$b = $this->getbuf();
			//var_dump($b);
			if ( $b === false ) { throw new Exception("Player count doesn't match! We're at ".$i."/".$players);return false; }
			$player = $b->fetch($cppl);
			//var_dump($player);
			$player['ip'] = ord($b->get()).".".ord($b->get()).".".ord($b->get()).".0";
			$p[] = $player;
		}
		$this->pinfo = $p;
		return true;
		//die();
	}
	public function getpong()
	{
		global $cppl;
		global $acp;
		$this->info = null;
		try 
		{
			$server = array();
			$server['players'] = array();
			while ( ($buf = $this->getbuf(false)) !== false )
			{
				$first = getint($buf);
				if ( $first === 0 )
				{
					for ( $i = 0; $i < 5; $i++ )
					{ getint($buf); }
					$mod = getint($buf);
					if ( $mod === 245 )
					{
						$player = $buf->fetch($cppl);
						
						$player['ip'] = ord($buf->get()).".".ord($buf->get()).".".ord($buf->get()).".0";
						$server['players'][] = $player;
					}
					else { var_dump($mod); }
				}
				elseif ( $first === 19 )
				{
					$sin = $buf->fetch($acp);
					foreach($sin as $k => $v ) { $server[$k] = $v; }
				}
				else { throw new Exception("Unknown result: ".$first); return false; }
			}
		}
		catch ( Exception $e )
		{
			throw $e;
			return true;
		}
	//	var_dump($server['players']);
		//var_dump($server);
		$this->info = $server;
		return true;
		$buf = $this->getbuf();
		$this->sinfo = null;
		$this->pinfo = null;
		$this->info = null;
		if ( $buf === false ) { return false; }
		$int = getint($buf);
		if ( $int === 19 )
		{
			$this->sinfo($buf);
		}
		elseif ( $int === 0 )
		{
			$this->pinfo($buf);
		}
		else { throw new Exception("Unknown: ".$fi); }
		//die( "HERE");
		$buf = $this->getbuf();
		if ( $buf === false ) { return false; }
		$int = getint($buf);
		if ( $int === 19 )
		{
			$this->sinfo($buf);
		}
		elseif ( $int === 0 )
		{
			$this->pinfo($buf);
		}
		else { throw new Exception("Unknown: ".$int); }
		$this->info = $this->sinfo;
		$this->info['players'] = $this->pinfo;
		return true;
		//elseif ( $fi 
		var_dump($fi);
		$sb = $this->getbuf();
		if ( $sb === false ) { return false; }
		$si = getint($sb);
		var_dump($si);
		$tb = $this->getbuf();
		if ( $tb === false ) { return false; }
		$ti = getint($tb);
		var_dump($ti);
		return false;
		global $acp;
		global $cppl;
		$w = $e = array();
		$r = array($this->sock);
		if ( stream_select($r,$w,$e, 0, 500) === 0 )
		{
			throw new Exception("No packet received...");
			return false;
		}
		$s = stream_socket_recvfrom($this->sock, 255);
		$this->info = null;
		if ( empty($s) || $s === false ) { throw new Exception("No value...!"); return false;}
		$main_info = new Buffer($s);
	//	var_dump($main_info);
		//var_dump($s);
		try
		{
			$server_info = $main_info->fetch($acp);
		}
		catch (Exception $e)
		{
		//	$z = true;
		//	echo "KK";
		//var_dump($main_info);
			throw new Exception("Failed to receive server data: ".$e->getMessage());
			
			return false;
		}
	//	if ( isset($z) )     { 
	//	var_dump($main_info);
	//}
		$players = array();
		// Get IDs...
		$r = array($this->sock);
		if ( stream_select($r,$w,$e, 0, 500) === 0 )
		{
			throw new Exception("No packet received...");
			return false;
		}
		stream_socket_recvfrom($this->sock, 255);
		for ( $i = 0; $i < $server_info['curplayers']; $i++)
		{
			$r = array($this->sock);
			if ( stream_select($r,$w,$e, 0, 500) === 0 )
			{
				throw new Exception("No predicted received...");
				return false;
			}
			$buf = new Buffer(stream_socket_recvfrom($this->sock, 255));
			try
			{
				$player = $buf->fetch($cppl);
			}
			catch (Exception $e)
			{
				throw new Exception("Failed to receive player data: ".$e->getMessage());
				return false;
			}
			$player['ip'] = ord($buf->get()).".".ord($buf->get()).".".ord($buf->get()).".0";
			$players[] = $player;
		}
		//var_dump($server_info);
		//var_dump($players);
		$this->info = $server_info;
		$this->info['players'] = $players;
		return true;
	}
}
class Pinger
{
	public $__servers = null;
	public function __construct($servers)
	{
		$this->__servers = $servers;
	}
	public function pings()
	{
		foreach($this->__servers as $server)
		{
			$server->ping();
		}
	}
}
class Buffer
{
	private $__data = '';
	public function fetch($desc)
	{
		
		$ret = array();
		foreach($desc as $k => $v)
		{
			if ( $v == 'i' )
			{
				$value = getint($this);
			}
			elseif ( $v == 's' )
			{
				//echo "S";
				$value = getstring($this);
				//echo "X";
			}
			else
			{
				$value = null;
			}
			if ( !isset($value) || $value === false )
			{
				throw new Exception("Value for '".$k."' was `".$value."`.");
				return false;
			}
			$ret[$k] = $value;
		}
		
		return $ret;
	}
	public function __construct($data)
	{
		//var_dump($data);
		$this->__data = $data;
	}
	public function get()
	{
		//echo "K";
		if (strlen($this->__data) == 0 )
		{
			return null;
		}
		//echo "Old: "; var_dump($this->__data); echo "\n";
		$r = substr($this->__data,0,1);
		if ( strlen($this->__data) == 1 )
		{
			$this->__data = '';
		}
		else
		{
			$this->__data = substr($this->__data,1,strlen($this->__data)-1);
		}
		
		//echo "New: "; var_dump($this->__data); echo "\n";
		return $r;
	}
}
function getint($buf)
{
	$f = $buf->get();
	
	if ( $f === null )
	{
		//echo "K";
		return null;
	}
	$c = ord($f);
	if ( $c === 128 )
	{
		$one = $buf->get();
		$two = $buf->get();
		if ( $one === null || $two===null) { return null ;}
		
		$n = ord($one);
		$n |= ord($two) << 8;
		return $n;
	}
	elseif ( $c === 129 )
	{
		
		$one = $buf->get();
		$two =$buf->get();
		$three =$buf->get();
		$four =$buf->get();
		if ( $one === null || $two === null || $three === null || $four === null ) { return null; }
		//echo "K";
		//if (!isset($one,$two,$three,$four)) { return null; }
		$n = ord($one);
		$n |= ord($two) << 8;
		$n |= ord($three) << 16;
		$n |= ord($four) << 24;
		return $n;
	}
	else
	{
		return $c;
	}
}
function getstring($buf)
{
	$ret = null;
	$t = 0;
	while (true)
	{
		//echo "A";
		$c = getint($buf);
		$t++;
		if ( $c === null ) 
		{
			//echo"Z";
			return $ret;
		}
		elseif ( $c === 0 && $ret === null )
		{
			//echo"Y";
			return '';
		}
		elseif ( $c === 0 )
		{
			//echo"X";
			//var_dump($ret);
			return $ret;
		}
		elseif ( $ret === null )
		{
			$ret = "";
		}
		if ($c < 255)
		{
			$ret .= chr($c);
		}
	}
}
?>
