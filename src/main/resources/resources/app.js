import React, { useEffect, useRef, useState } from 'react'
import { render } from 'react-dom'

/**
 * @typedef storeFunction
 * @function
 * @param string... key, value
 * @return {string} the value, when the value argument is not provided
 */

/**
 * @param storage
 * @return storeFunction
 */
function storageFactory(storage) {
  return (key, value = undefined) => {
    if (value === undefined) {
      return storage.getItem(key);
    } else {
      if (value === null) {
        storage.removeItem(key);
      } else {
        storage.setItem(key, value);
      }
    }
  }
}

/**
 * @type {Object<string, storeFunction>}
 */
let storage = {
  local: storageFactory(localStorage),
  session: storageFactory(sessionStorage),
}

/**
 * @param setter {(function(*): void)}
 * @param key a
 * @param storage a
 * @return {(function(*): void)}
 */
function wrapWithStorage(setter, key, storage) {
  return value => {
    storage(key, value);
    setter(value);
  }
}

let storagePrefix = 'tic-tac-toe.';
let previousNameKey = storagePrefix + 'previousName';
let previousName = storage.local(previousNameKey);
console.log('Previous settings:', { previousName })

function App() {
  let [name, set_name] = useState(null);
  let setNameAndStore = wrapWithStorage(set_name, previousNameKey, storage.local);
  return <>
    <div>
      <Header />
      <div className="container">
        {name
          ? <MainScreen name={name} clearName={() => setNameAndStore(null)} />
          : <NickNameSetup set_name={setNameAndStore} />
        }
      </div>
    </div>
  </>;
}

render(<App />, document.body);

function NickNameSetup({ set_name }) {
  let [nick, set_nick] = useState(previousName);

  let inputNickRef = useAutoFocusRef();

  return <>
    <div className="row">
      <div className="col">
        <div className="ms-3 me-3 m-1 p-1 p-md-5 mb-4 bg-light rounded-3">
          <div className="container-fluid py-5">
            <h1 className="display-5 fw-bold">Welcome{previousName ? ' back' : ''} to Tic Tac Toe</h1>
            <p className="fs-4">Please {previousName ? 'confirm your' : 'enter a'} nickname to begin:</p>
            <form
              onSubmit={e => {
                e.preventDefault()
                set_name(nick)
              }}
            >
              <input type="text" ref={inputNickRef} value={nick} onChange={e => set_nick(e.target.value)} />
              <div className='mb-3'></div>
              <button className="btn btn-primary btn-lg" type="submit">Start
                playing{nick ? ' as: ' + nick : ''}</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </>
}

function useAutoFocusRef() {
  let matchMeRef = useRef(null);
  useEffect(() => {
    matchMeRef.current && matchMeRef.current.focus();
  }, [matchMeRef.current])
  return matchMeRef;
}

function MainScreen({ name, clearName }) {
  let [playing, set_playing] = useState(false);
  if (playing) return <MainScreenPlaying />;
  return <MainScreenStart name={name} clearName={clearName} startPlaying={() => set_playing(true)} />
}

function MainScreenPlaying() {
  let [match, set_match] = useState(null);
  let [id, set_id] = useState(null);

  set_match = wrapWithStorage(set_match, storagePrefix + 'match', storage.session);
  set_id = wrapWithStorage(set_id, storagePrefix + 'id', storage.session);

  useEffect(() => {
    if (id) return

    const sse = new EventSource('/api/queue', { withCredentials: true });

    sse.addEventListener('id', e => set_id(JSON.parse(e.data).id))
    sse.addEventListener('gameId', e => set_match(JSON.parse(e.data)))

    sse.onerror = e => {
      console.error(e)
      sse.close();
    }
    return () => sse.close();
  });

  function clear() {
    set_id(null);
    set_match(null);
  }

  if (match) {
    // noinspection JSUnusedGlobalSymbols
    return <MainScreenGame {...{ id, match, clear }} />;
  }

  return <p>you are id {id || '(loading...)'}, waiting for someone else to join</p>
}

/**
 * @param props
 * @param props.id
 * @param props.match
 * @param props.clear
 */
function MainScreenGame({ id, match, clear }) {
  let [board, set_board] = useState();
  set_board = wrapWithStorage(set_board, storagePrefix + 'board', sessionStorage);

  let { gameId, player } = match;
  useEffect(() => {
    const sse = new EventSource(`/api/game/${gameId}/events`, { withCredentials: true });
    sse.addEventListener('board', e => set_board(JSON.parse(e.data)))
    sse.onerror = e => console.error(e) || sse.close();
    return () => sse.close();
  })
  return <>
    <p>you player {player} in game {gameId}.</p>
    <p>your id is {id}.</p>
    <p>the board is:</p>
    <pre>{board && JSON.stringify(board, null, 2) || null}</pre>
    <button onClick={clear}>exit this match</button>
  </>;
}


function MainScreenStart({ name, clearName, startPlaying }) {
  let matchMeRef = useAutoFocusRef();
  return <>
    <RowWithCardBody>
      <p className="card-text">
        Welcome, {name}, lets begin!
      </p>
      <button ref={matchMeRef} type="button" className="btn btn-primary" onClick={startPlaying}>
        Match me with another player
      </button>
    </RowWithCardBody>
    <RowWithCardBody>
      <button type="button" className="btn btn-primary" onClick={clearName}>Change nickname</button>
    </RowWithCardBody>
  </>
}

function Header() {
  return <>
    <div>
      <header className="d-flex flex-wrap justify-content-center py-3 mb-4 border-bottom">
        <a href="/" className="d-flex align-items-center mb-3 mb-md-0 me-md-auto text-dark text-decoration-none">
          {/*<svg className="bi me-2" width="40" height="32"><use xlink:href="#bootstrap"/></svg>*/}
          <img src="favicon.ico" className="bi ms-2 me-2" height="32" alt="" />
          <span className="fs-4">Tic Tac Toe</span>
        </a>

        <ul className="nav nav-pills">
          <li className="nav-item"><a href="#" className="nav-link active" aria-current="page">Home</a></li>
          <li className="nav-item"><a href="https://github.com/alexanderankin" className="nav-link">Repository</a>
          </li>
        </ul>
      </header>
    </div>
  </>;
}

function RowWithCardBody({ children }) {
  return <>
    <div className="row">
      <div className="col col-md-6 col-lg-4">
        <div className="card">
          <div className="card-body">
            {children}
          </div>
        </div>
      </div>
    </div>
  </>;
}
